package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.CommunityEntry
import com.krelinnbios.neodblite.data.model.CommunityEntryType

object CommunityHtmlParser {
    private val sectionRegex = Regex("""<section\b[\s\S]*?</section>""", RegexOption.IGNORE_CASE)
    private val nicknameRegex = Regex("""class=["']nickname["'][^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
    private val actionRegex = Regex("""<span\s+class=["']action inline["']>[\s\S]*?<span\s+class=["']timestamp["']>([\s\S]*?)</span>""", RegexOption.IGNORE_CASE)
    private val commentRegex = Regex("""<span\s+id=["']comment_[^"']+_content["'][^>]*>([\s\S]*?)</span>""", RegexOption.IGNORE_CASE)
    private val tldrRegex = Regex("""<div\s+class=["']tldr[^"']*["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
    private val absoluteHrefRegex = Regex("""href=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
    private val relativeHrefRegex = Regex("""href=["'](/(?:review|@|users|piece|post)/[^"']+)["']""", RegexOption.IGNORE_CASE)
    private val ratingRegex = Regex("""class=["']rating-star["']\s+data-rating=["']([\d.]+)["']""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""class=["']post_timestamp["'][\s\S]*?<a\b[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("""<[^>]+>""")
    private val whitespaceRegex = Regex("""\s+""")

    private val relativeAgeRegex = Regex("""^(\d+)\s*(yr|mo|w|d|h|m|s)$""", RegexOption.IGNORE_CASE)

    /**
     * 把站点展示的紧凑相对时间（如 "7d"、"2w"、"1mo"）换算成分钟数，用于跨类型按新旧排序；
     * 解析不到返回 null。换算取近似值即可，只需保证单位间的大小关系正确。
     */
    fun relativeAgeMinutes(date: String?): Long? {
        val text = date?.trim().orEmpty()
        if (text.isEmpty()) return null
        val match = relativeAgeRegex.find(text) ?: return null
        val value = match.groupValues[1].toLongOrNull() ?: return null
        return when (match.groupValues[2].lowercase()) {
            "s" -> value / 60
            "m" -> value
            "h" -> value * 60
            "d" -> value * 60 * 24
            "w" -> value * 60 * 24 * 7
            "mo" -> value * 60 * 24 * 30
            "yr" -> value * 60 * 24 * 365
            else -> null
        }
    }

    fun parse(type: CommunityEntryType, html: String, host: String): List<CommunityEntry> {
        if (html.isBlank() || html.contains("class=\"empty\"") || html.contains("class='empty'")) return emptyList()
        return sectionRegex.findAll(html)
            .mapNotNull { parseSection(type, it.value, host) }
            .take(60)
            .toList()
    }

    private fun parseSection(type: CommunityEntryType, section: String, host: String): CommunityEntry? {
        val contentHtml = when (type) {
            CommunityEntryType.COMMENT -> commentRegex.find(section)?.groupValues?.getOrNull(1)
            CommunityEntryType.REVIEW, CommunityEntryType.NOTE -> tldrRegex.find(section)?.groupValues?.getOrNull(1)
        } ?: tldrRegex.find(section)?.groupValues?.getOrNull(1)

        val content = clean(contentHtml.orEmpty())
        if (content.isBlank()) return null

        val author = clean(nicknameRegex.find(section)?.groupValues?.getOrNull(1).orEmpty())
        val action = clean(actionRegex.find(section)?.groupValues?.getOrNull(1).orEmpty())
        val url = absoluteHrefRegex.find(section)?.groupValues?.getOrNull(1)
            ?: relativeHrefRegex.find(section)?.groupValues?.getOrNull(1)?.let { "https://$host$it" }
        val rating = ratingRegex.find(section)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val date = dateRegex.find(section)?.groupValues?.getOrNull(1)
            ?.let { clean(it) }?.takeIf { it.isNotBlank() }

        return CommunityEntry(
            type = type,
            author = author,
            action = action,
            content = content,
            url = url,
            rating = rating,
            date = date
        )
    }

    private fun clean(html: String): String {
        val withoutTags = html
            .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), " ")
            .replace(tagRegex, " ")
        return decodeEntities(withoutTags)
            .replace(whitespaceRegex, " ")
            .trim()
    }

    private fun decodeEntities(value: String): String = value
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("""&#(\d+);""")) { match ->
            match.groupValues[1].toIntOrNull()?.let { code ->
                runCatching { String(Character.toChars(code)) }.getOrNull()
            } ?: match.value
        }
}