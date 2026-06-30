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
    private val tagRegex = Regex("""<[^>]+>""")
    private val whitespaceRegex = Regex("""\s+""")

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

        return CommunityEntry(
            type = type,
            author = author,
            action = action,
            content = content,
            url = url,
            rating = rating
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