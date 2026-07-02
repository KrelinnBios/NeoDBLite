package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.CommunityEntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityHtmlParserTest {
    @Test
    fun parsesCompactRelativeAges() {
        assertEquals(0L, CommunityHtmlParser.relativeAgeMinutes("30s"))
        assertEquals(45L, CommunityHtmlParser.relativeAgeMinutes("45m"))
        assertEquals(120L, CommunityHtmlParser.relativeAgeMinutes("2h"))
        assertEquals(7L * 60 * 24, CommunityHtmlParser.relativeAgeMinutes("7d"))
        assertEquals(2L * 60 * 24 * 7, CommunityHtmlParser.relativeAgeMinutes("2w"))
        assertEquals(60L * 24 * 30, CommunityHtmlParser.relativeAgeMinutes("1mo"))
        assertEquals(60L * 24 * 365, CommunityHtmlParser.relativeAgeMinutes("1yr"))
    }

    @Test
    fun toleratesWhitespaceAndCase() {
        assertEquals(60L * 24 * 30, CommunityHtmlParser.relativeAgeMinutes(" 1MO "))
        assertEquals(60L, CommunityHtmlParser.relativeAgeMinutes("1 h"))
    }

    @Test
    fun unitOrderingIsMonotonic() {
        val ages = listOf("59s", "59m", "23h", "6d", "3w", "11mo", "2yr")
            .map { CommunityHtmlParser.relativeAgeMinutes(it)!! }
        assertTrue(ages == ages.sorted())
    }

    @Test
    fun rejectsUnparseableDates() {
        assertNull(CommunityHtmlParser.relativeAgeMinutes(null))
        assertNull(CommunityHtmlParser.relativeAgeMinutes(""))
        assertNull(CommunityHtmlParser.relativeAgeMinutes("昨天"))
        assertNull(CommunityHtmlParser.relativeAgeMinutes("2024-01-01"))
        assertNull(CommunityHtmlParser.relativeAgeMinutes("7x"))
    }

    @Test
    fun parsesCommentSectionAndNormalizesFields() {
        val html = """
            <section>
                <a class="nickname">Alice &amp; Bob</a>
                <span class="action inline">commented <span class="timestamp">2h</span></span>
                <a href="/users/alice/comment/123">open</a>
                <span class="rating-star" data-rating="8.0"></span>
                <span id="comment_abc_content">Hello&nbsp;<b>NeoDB</b>&#33;</span>
                <span class="post_timestamp"><a>2h</a></span>
            </section>
        """.trimIndent()

        val entry = CommunityHtmlParser.parse(CommunityEntryType.COMMENT, html, "neodb.social").single()

        assertEquals(CommunityEntryType.COMMENT, entry.type)
        assertEquals("Alice & Bob", entry.author)
        assertEquals("2h", entry.action)
        assertEquals("Hello NeoDB!", entry.content)
        assertEquals("https://neodb.social/users/alice/comment/123", entry.url)
        assertEquals(8.0, entry.rating!!, 0.0)
        assertEquals("2h", entry.date)
    }

    @Test
    fun parsesReviewTldrAndKeepsAbsoluteUrl() {
        val html = """
            <section>
                <a class="nickname">Reviewer</a>
                <span class="action inline">reviewed <span class="timestamp">1d</span></span>
                <a href="https://example.org/review/42">open</a>
                <div class="tldr markdown">Great &lt;read&gt;.</div>
                <span class="post_timestamp"><a>1d</a></span>
            </section>
        """.trimIndent()

        val entry = CommunityHtmlParser.parse(CommunityEntryType.REVIEW, html, "neodb.social").single()

        assertEquals(CommunityEntryType.REVIEW, entry.type)
        assertEquals("Reviewer", entry.author)
        assertEquals("Great <read>.", entry.content)
        assertEquals("https://example.org/review/42", entry.url)
        assertNull(entry.rating)
        assertEquals("1d", entry.date)
    }

    @Test
    fun parsesNoteTldrWithRelativePieceUrl() {
        val html = """
            <section>
                <a class="nickname">Note Writer</a>
                <span class="action inline">noted <span class="timestamp">30m</span></span>
                <a href="/piece/abc">open</a>
                <div class="tldr">Short <em>note</em>.</div>
                <span class="post_timestamp"><a>30m</a></span>
            </section>
        """.trimIndent()

        val entry = CommunityHtmlParser.parse(CommunityEntryType.NOTE, html, "neodb.social").single()

        assertEquals(CommunityEntryType.NOTE, entry.type)
        assertEquals("Note Writer", entry.author)
        assertEquals("Short note.", entry.content)
        assertEquals("https://neodb.social/piece/abc", entry.url)
        assertEquals("30m", entry.date)
    }

    @Test
    fun ignoresEmptyAndContentlessSections() {
        assertTrue(CommunityHtmlParser.parse(CommunityEntryType.COMMENT, """<div class="empty"></div>""", "neodb.social").isEmpty())
        assertTrue(
            CommunityHtmlParser.parse(
                CommunityEntryType.COMMENT,
                """<section><a class="nickname">Nobody</a></section>""",
                "neodb.social"
            ).isEmpty()
        )
    }
}
