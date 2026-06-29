package com.krelinnbios.neodblite.global

import kotlinx.coroutines.flow.MutableStateFlow

/** OAuth 回调 code 的进程内传递通道：MainActivity 接到 deep link 后投递，Compose 侧消费。 */
object OAuthBus {
    val pendingCode = MutableStateFlow<String?>(null)
}
