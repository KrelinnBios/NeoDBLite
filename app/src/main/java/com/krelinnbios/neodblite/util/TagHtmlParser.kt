package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.Category
import okhttp3.HttpUrl

/** 从实例的用户标签页读取分类计数。无法确认页面或分类时由调用方回退到登录 API。 */
internal object TagHtmlParser {
    private val list = Regex("""<div\s+class="tag-list"\s*>([\s\S]*?)</div>""")
    private val entry = Regex("""<span\s+class="tag-list-entry"\s*>""")
    private val link = Regex("""<a\s+href="([^"]+)"""")
    private val count = Regex("""</a>\s*</span>\s*<span>\s*\(([0-9][0-9,.\s\u00a0\u202f]*)\)\s*</span>""")
    private val selector = Regex("""<select\b[^>]*\bname="category"[^>]*>([\s\S]*?)</select>""")
    private val selected = Regex("""<option\b(?=[^>]*\sselected\b)[^>]*\svalue="([^"]*)"[^>]*>""")

    fun counts(html: String, page: HttpUrl, category: Category?): Map<String, Int>? {
        val options = selector.find(html)?.groupValues?.get(1) ?: return null
        val value = selected.find(options)?.groupValues?.get(1) ?: return null
        if (value != category?.apiValue.orEmpty()) return null
        val body = list.find(html)?.groupValues?.get(1) ?: return null
        val result = mutableMapOf<String, Int>()
        for (row in body.split(entry).drop(1)) {
            val href = link.find(row)?.groupValues?.get(1) ?: return null
            val url = page.resolve(href.replace("&amp;", "&")) ?: return null
            if (url.host != page.host || url.encodedPath.trimEnd('/').substringBeforeLast('/') !=
                page.encodedPath.trimEnd('/') || url.queryParameter("category") != category?.apiValue) return null
            val title = url.pathSegments.lastOrNull { it.isNotEmpty() } ?: return null
            val total = count.find(row)?.groupValues?.get(1)?.filter(Char::isDigit)?.toIntOrNull() ?: return null
            result[title] = total
        }
        // 有新结构的链接却没有可识别的条目时，不把不兼容的页面当成空分类。
        return result.takeUnless { it.isEmpty() && "<a " in body }
    }
}
