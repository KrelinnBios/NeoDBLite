package com.krelinnbios.neodblite.global

import android.content.Context
import com.krelinnbios.neodblite.data.AuthRepository
import com.krelinnbios.neodblite.data.AuthStore
import com.krelinnbios.neodblite.data.NeoDBClient
import com.krelinnbios.neodblite.data.NeoDBRepository
import com.krelinnbios.neodblite.data.SearchHistoryStore

/**
 * 极简手动依赖容器。在 Application 中初始化一次，全局单例通过 [App] 暴露。
 * 这样 ViewModel 无需复杂的注入框架即可拿到仓库。
 */
class AppContainer(context: Context) {
    val authStore = AuthStore(context.applicationContext)
    val client = NeoDBClient(authStore)
    val authRepository = AuthRepository(authStore, client)
    val repository = NeoDBRepository(client)
    val searchHistory = SearchHistoryStore(context)

    /** 启动时载入持久化的 host/token，并据 host 配置网络客户端。 */
    suspend fun bootstrap() {
        authStore.load()
        client.configure(authStore.cachedHost)
    }
}

object App {
    lateinit var container: AppContainer
        private set

    fun init(context: Context) {
        if (!::container.isInitialized) {
            container = AppContainer(context)
        }
    }
}
