package com.krelinnbios.neodblite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.krelinnbios.neodblite.data.model.NeoUser
import kotlinx.coroutines.flow.firstOrNull

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "neodb_auth")

/**
 * 鉴权与实例信息的持久化存储。
 * - 当前实例 host、access_token；
 * - 每个实例独立的 OAuth 应用凭据（client_id / client_secret），按 host 区分。
 *
 * token 与 host 加载后缓存在内存，供 OkHttp 拦截器同步读取（拦截器不能挂起）。
 */
class AuthStore(private val context: Context) {

    @Volatile var cachedHost: String = DEFAULT_HOST
        private set

    @Volatile var cachedToken: String? = null
        private set

    /** 上次成功获取的当前用户，供网络异常时保持登录、先进入应用。 */
    @Volatile var cachedUser: NeoUser? = null
        private set

    private val store get() = context.authDataStore
    private val gson = Gson()

    suspend fun load() {
        val prefs = store.data.firstOrNull()
        cachedHost = prefs?.get(KEY_HOST)?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
        cachedToken = prefs?.get(KEY_TOKEN)?.takeIf { it.isNotBlank() }
        cachedUser = prefs?.get(KEY_USER)?.takeIf { it.isNotBlank() }?.let {
            runCatching { gson.fromJson(it, NeoUser::class.java) }.getOrNull()
        }
    }

    suspend fun saveUser(user: NeoUser) {
        cachedUser = user
        runCatching { store.edit { it[KEY_USER] = gson.toJson(user) } }
    }

    suspend fun setHost(host: String) {
        val normalized = normalizeHost(host)
        cachedHost = normalized
        store.edit { it[KEY_HOST] = normalized }
    }

    suspend fun saveClientCredentials(host: String, clientId: String, clientSecret: String) {
        val h = normalizeHost(host)
        store.edit {
            it[clientIdKey(h)] = clientId
            it[clientSecretKey(h)] = clientSecret
        }
    }

    suspend fun clientCredentials(host: String): Pair<String, String>? {
        val h = normalizeHost(host)
        val prefs = store.data.firstOrNull() ?: return null
        val id = prefs[clientIdKey(h)] ?: return null
        val secret = prefs[clientSecretKey(h)] ?: return null
        return id to secret
    }

    suspend fun saveToken(token: String) {
        cachedToken = token
        store.edit { it[KEY_TOKEN] = token }
    }

    suspend fun clearToken() {
        cachedToken = null
        cachedUser = null
        store.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER)
        }
    }

    companion object {
        const val DEFAULT_HOST = "neodb.social"

        private val KEY_HOST = stringPreferencesKey("instance_host")
        private val KEY_TOKEN = stringPreferencesKey("access_token")
        private val KEY_USER = stringPreferencesKey("cached_user")
        private fun clientIdKey(host: String) = stringPreferencesKey("client_id@$host")
        private fun clientSecretKey(host: String) = stringPreferencesKey("client_secret@$host")

        /** 去掉协议、路径与首尾空白，仅保留主机名。 */
        fun normalizeHost(raw: String): String {
            var h = raw.trim()
            h = h.removePrefix("https://").removePrefix("http://")
            h = h.substringBefore('/')
            h = h.trim().trimEnd('.')
            return h.ifBlank { DEFAULT_HOST }
        }
    }
}
