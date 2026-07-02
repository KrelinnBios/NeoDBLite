package com.krelinnbios.neodblite.util

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
    fun toleratesWhitespaceAndCase(): Unit {
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
}
