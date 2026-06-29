package com.krelinnbios.neodblite.util

object ProfileHtmlParser {
    private val profileSectionRegex = Regex(
        """<section\b(?=[^>]*class=['\"][^'\"]*\bprofile\b[^'\"]*['\"])[^>]*>([\s\S]*?)</section>""",
        RegexOption.IGNORE_CASE
    )
    private val preferredBlockRegex = Regex(
        """<(div|section|article)\b(?=[^>]*(?:class|id)=['\"][^'\"]*(?:bio|intro|summary|about|description|note)[^'\"]*['\"])[^>]*>([\s\S]*?)</\1>""",
        RegexOption.IGNORE_CASE
    )
    private val divBlockRegex = Regex("""<div\b[^>]*>[\s\S]*?</div>""", RegexOption.IGNORE_CASE)
    private val summaryRegex = Regex("""<summary\b[\s\S]*?</summary>""", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("""<[^>]+>""")
    private val paragraphCloseRegex = Regex("""</p\s*>""", RegexOption.IGNORE_CASE)
    private val paragraphOpenRegex = Regex("""<p\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val breakRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val scriptRegex = Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE)
    private val styleRegex = Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE)
    private val whitespaceRegex = Regex("""[ \t\x0B\f\r]+""")
    private val handleLineRegex = Regex("""^@?[A-Za-z0-9_.-]+@[A-Za-z0-9.-]+$""")

    fun parseBio(html: String): String? {
        if (html.isBlank()) return null

        val candidates = buildList {
            profileSectionRegex.findAll(html).forEach { match ->
                val bodyOnly = summaryRegex.replace(match.value, " ")
                divBlockRegex.findAll(bodyOnly).forEach { div ->
                    add(Candidate(div.value, preferred = true))
                }
            }
            preferredBlockRegex.findAll(html).forEach { match ->
                if (!match.value.contains("class=\"profile\"", ignoreCase = true) &&
                    !match.value.contains("class='profile'", ignoreCase = true)
                ) {
                    add(Candidate(match.value, preferred = false))
                }
            }
            divBlockRegex.findAll(html).forEach { match ->
                add(Candidate(match.value, preferred = false))
            }
        }

        return candidates
            .mapNotNull { candidate ->
                val text = clean(candidate.html)
                if (!isLikelyBio(candidate.html, text)) return@mapNotNull null
                ScoredText(text, score(candidate, text))
            }
            .maxByOrNull { it.score }
            ?.text
            ?.takeIf { it.isNotBlank() }
    }

    private fun isLikelyBio(html: String, text: String): Boolean {
        if (text.isBlank()) return false
        if (text.length > 4_000) return false
        if (!html.contains("<p", ignoreCase = true) && !html.contains("<br", ignoreCase = true)) return false
        val lower = html.lowercase()
        val rejectedTokens = listOf(
            "<nav", "<form", "<input", "<button", "<hgroup", "class=\"nickname",
            "class='nickname", "class=\"handler", "class='handler", "post_author",
            "activity post", "pagination", "dropdown", "tabs"
        )
        if (rejectedTokens.any { it in lower }) return false
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isNotEmpty() && lines.all { isIdentityLine(it) }) return false
        return true
    }

    private fun score(candidate: Candidate, text: String): Int {
        var value = text.length.coerceAtMost(600)
        if (candidate.preferred) value += 1_000
        value += Regex("""<p\b""", RegexOption.IGNORE_CASE).findAll(candidate.html).count() * 120
        if (candidate.html.contains("<br", ignoreCase = true)) value += 80
        return value
    }

    private fun clean(html: String): String {
        val withLineBreaks = html
            .replace(scriptRegex, " ")
            .replace(styleRegex, " ")
            .replace(breakRegex, "\n")
            .replace(paragraphOpenRegex, "")
            .replace(paragraphCloseRegex, "\n")
            .replace(tagRegex, " ")
        return decodeEntities(withLineBreaks)
            .lines()
            .map { it.replace(whitespaceRegex, " ").trim() }
            .filter { it.isNotBlank() }
            .filterNot { isIdentityLine(it) }
            .joinToString("\n")
    }

    private fun isIdentityLine(line: String): Boolean {
        val value = line.trim()
        if (value.isBlank()) return true
        if (handleLineRegex.matches(value)) return true
        return false
    }

    private fun decodeEntities(value: String): String = value
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace(Regex("""&#(\d+);""")) { match ->
            match.groupValues[1].toIntOrNull()?.let { code ->
                runCatching { String(Character.toChars(code)) }.getOrNull()
            } ?: match.value
        }
        .replace(Regex("""&#x([0-9a-fA-F]+);""")) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { code ->
                runCatching { String(Character.toChars(code)) }.getOrNull()
            } ?: match.value
        }

    private data class Candidate(val html: String, val preferred: Boolean)
    private data class ScoredText(val text: String, val score: Int)
}
