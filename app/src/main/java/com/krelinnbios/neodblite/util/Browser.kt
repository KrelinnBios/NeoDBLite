package com.krelinnbios.neodblite.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object Browser {
    /** 优先用 Chrome Custom Tab 打开，失败回退系统浏览器。 */
    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        try {
            CustomTabsIntent.Builder().build().launchUrl(context, uri)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                // 无可用浏览器，静默忽略；调用方已有提示路径。
            }
        }
    }
}
