package com.krelinnbios.neodblite.data

import com.krelinnbios.neodblite.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
        .callTimeout(60, TimeUnit.SECONDS)
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
