package com.krelinnbios.neodblite.data

import com.krelinnbios.neodblite.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 维护指向当前实例的 Retrofit/OkHttp。host 或 token 变化时调用 [configure] 重建 [api]。
 * token 从 [AuthStore] 的内存缓存同步读取，拦截器据此附加 Authorization 头。
 */
class NeoDBClient(private val authStore: AuthStore) {

    @Volatile
    private var currentHost: String = authStore.cachedHost

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // 网络不稳定时，连接级失败自动重试；配合 RetryInterceptor 处理读超时/瞬时 5xx。
        .retryOnConnectionFailure(true)
        .addInterceptor(RetryInterceptor(maxRetries = 2))
        .addInterceptor { chain ->
            val builder: Request.Builder = chain.request().newBuilder()
                .header("User-Agent", "NeoDBLite/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/json")
            authStore.cachedToken?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
        .build()

    @Volatile
    var api: NeoDBApi = buildApi(currentHost)
        private set

    /** host 变化时重建 Retrofit（baseUrl 不可变）。token 变化无需重建，拦截器读缓存即可。 */
    @Synchronized
    fun configure(host: String) {
        val normalized = AuthStore.normalizeHost(host)
        if (normalized == currentHost) return
        currentHost = normalized
        api = buildApi(normalized)
    }

    private fun buildApi(host: String): NeoDBApi =
        Retrofit.Builder()
            .baseUrl("https://$host/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NeoDBApi::class.java)
}

/**
 * 仅对幂等的 GET 请求做有限重试：遇到网络 IO 异常或瞬时 5xx 时，退避后重试，
 * 缓解网络不稳定造成的偶发失败。非 GET 请求不重试，避免重复提交。
 */
private class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var lastError: IOException? = null
        var attempt = 0
        while (attempt <= maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)
                lastError = null
                if (response.isSuccessful || request.method != "GET" || response.code < 500) {
                    return response
                }
            } catch (error: IOException) {
                lastError = error
                if (request.method != "GET") throw error
            }
            attempt++
            if (attempt <= maxRetries) {
                try {
                    Thread.sleep(300L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        lastError?.let { throw it }
        return response!!
    }
}
