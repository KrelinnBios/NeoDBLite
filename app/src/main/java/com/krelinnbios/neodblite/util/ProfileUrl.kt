package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.NeoUser

/** 将接口返回的绝对/相对用户地址统一为浏览器可打开的 HTTPS 地址。 */
fun profileWebUrl(user: NeoUser, host: String): String? {
    val normalizedHost = host
        .trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .trimEnd('/')
        .takeIf { it.isNotBlank() }
        ?: return null

    user.url?.trim()?.takeIf { it.isNotBlank() }?.let { rawUrl ->
        if (rawUrl.startsWith("https://", ignoreCase = true) ||
            rawUrl.startsWith("http://", ignoreCase = true)
        ) {
            return rawUrl
        }
        if (rawUrl.startsWith("//")) return "https:$rawUrl"

        val pathOrHost = rawUrl.trimStart('/')
        return if (pathOrHost == normalizedHost || pathOrHost.startsWith("$normalizedHost/")) {
            "https://$pathOrHost"
        } else {
            "https://$normalizedHost/$pathOrHost"
        }
    }

    val username = user.username
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() }
        ?: user.externalAcct
            ?.trim()
            ?.removePrefix("@")
            ?.substringBefore('@')
            ?.takeIf { it.isNotBlank() }
        ?: return null
    return "https://$normalizedHost/users/$username"
}
