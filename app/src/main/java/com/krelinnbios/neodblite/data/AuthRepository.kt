package com.krelinnbios.neodblite.data

import android.net.Uri
import com.google.gson.Gson
import com.krelinnbios.neodblite.data.model.AppRegistration
import com.krelinnbios.neodblite.data.model.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * NeoDB 走 Mastodon 兼容的 OAuth 授权码流程：
 * 1. 按实例注册应用（POST /api/v1/apps）拿到 client_id/secret，按 host 持久化复用；
 * 2. 打开 /oauth/authorize 让用户授权，实例重定向回 [REDIRECT_URI] 带 code；
 * 3. 用 code 换 access_token（POST /oauth/token）。
 */
class AuthRepository(
    private val authStore: AuthStore,
    private val client: NeoDBClient
) {
    private val gson = Gson()
    private val http: OkHttpClient get() = client.okHttpClient

    /** 为指定实例准备 OAuth 应用凭据（已有则复用），返回授权页 URL。 */
    suspend fun beginLogin(host: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = AuthStore.normalizeHost(host)
            authStore.setHost(normalized)
            client.configure(normalized)

            val (clientId, _) = authStore.clientCredentials(normalized)
                ?: registerApp(normalized).also {
                    authStore.saveClientCredentials(normalized, it.first, it.second)
                }

            val authorizeUrl = Uri.Builder()
                .scheme("https")
                .authority(normalized)
                .appendEncodedPath("oauth/authorize")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("scope", SCOPE)
                .build()
                .toString()
            authorizeUrl
        }
    }

    /** 注册 OAuth 应用，返回 (clientId, clientSecret)。 */
    private fun registerApp(host: String): Pair<String, String> {
        val form = FormBody.Builder()
            .add("client_name", APP_NAME)
            .add("redirect_uris", REDIRECT_URI)
            .add("scopes", SCOPE)
            .add("website", WEBSITE)
            .build()
        val request = Request.Builder()
            .url("https://$host/api/v1/apps")
            .post(form)
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("注册应用失败：HTTP ${response.code}")
            }
            val reg = gson.fromJson(body, AppRegistration::class.java)
                ?: throw IOException("注册应用返回无法解析")
            return reg.clientId to reg.clientSecret
        }
    }

    /** 用授权 code 换取并保存 access_token。 */
    suspend fun completeLogin(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val host = authStore.cachedHost
            val creds = authStore.clientCredentials(host)
                ?: throw IOException("缺少应用凭据，请重新登录")
            val (clientId, clientSecret) = creds

            val form = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("scope", SCOPE)
                .build()
            val request = Request.Builder()
                .url("https://$host/oauth/token")
                .post(form)
                .build()
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("获取令牌失败：HTTP ${response.code}")
                }
                val token = gson.fromJson(body, TokenResponse::class.java)
                    ?: throw IOException("令牌返回无法解析")
                if (token.accessToken.isBlank()) throw IOException("令牌为空")
                authStore.saveToken(token.accessToken)
            }
        }
    }

    suspend fun logout() {
        authStore.clearToken()
    }

    companion object {
        const val APP_NAME = "NeoDB Lite"
        const val WEBSITE = "https://github.com/KrelinnBios/NeoDBLite"
        const val REDIRECT_URI = "neodblite://oauth/callback"
        const val SCOPE = "read write"
    }
}
