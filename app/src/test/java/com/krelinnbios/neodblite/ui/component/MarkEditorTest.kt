package com.krelinnbios.neodblite.ui.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.gson.Gson
import com.krelinnbios.neodblite.data.model.MarkInRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MarkEditorTest {
    @Test
    fun sliderValueToGradeRoundsToNearestGrade() {
        assertEquals(0, sliderValueToGrade(0f))
        assertEquals(5, sliderValueToGrade(4.9999995f))
        assertEquals(6, sliderValueToGrade(5.9999995f))
        assertEquals(10, sliderValueToGrade(10f))
    }

    @Test
    fun sliderValueToGradeClampsToSupportedRange() {
        assertEquals(0, sliderValueToGrade(-1f))
        assertEquals(10, sliderValueToGrade(11f))
    }

    @Test
    fun tagSuggestionReplacesPartialTag() {
        val result = completeMarkTag(input("read sci"), "science")

        assertEquals("read science ", result.text)
        assertEquals(listOf("read", "science"), parseMarkTags(result.text))
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun tagSuggestionAppendsAfterSeparators() {
        listOf("", "read ", "read,", "read，", "read、").forEach { text ->
            val result = completeMarkTag(input(text), "science")
            val expected = if (text.isEmpty()) "science " else "read science "
            assertEquals(expected, result.text)
            assertEquals(TextRange(result.text.length), result.selection)
        }
    }

    @Test
    fun tagSuggestionReplacesTokenAtCursorAndPreservesOtherTags() {
        val value = TextFieldValue("read sci favorite", selection = TextRange(7))

        assertEquals("sci", markTagQuery(value))
        assertEquals("read science favorite ", completeMarkTag(value, "science").text)
    }

    @Test
    fun tagSuggestionReplacesSelectedToken() {
        val value = TextFieldValue("read sci favorite", selection = TextRange(5, 8))

        assertEquals("read science favorite ", completeMarkTag(value, "science").text)
    }

    @Test
    fun tagSuggestionHandlesHashPrefixAndExistingTag() {
        val value = input("#science #sci")

        assertEquals("sci", markTagQuery(value))
        assertEquals("science ", completeMarkTag(value, "science").text)
        assertEquals("science ", completeMarkTag(input("science"), "science").text)
    }

    @Test
    fun tagSuggestionCompletesChinesePartialTag() {
        val result = completeMarkTag(input("已读 科"), "科幻")

        assertEquals(listOf("已读", "科幻"), parseMarkTags(result.text))
    }

    @Test
    fun emptyTagQueryAllowsAnotherSuggestionAfterCompletion() {
        assertEquals("", markTagQuery(input("read ")))
        assertEquals("", markTagQuery(input("")))
    }

    @Test
    fun markDateSerializesAsFullUtcTimestamp() {
        assertEquals("2024-02-29T00:00:00Z", markDateToCreatedTime("2024-02-29"))
        assertEquals("2026-09-14T00:00:00Z", markDateToCreatedTime(" 2026-09-14 "))
    }

    @Test
    fun markDateRejectsInvalidCalendarDates() {
        listOf("2026-02-29", "2026-02-30", "2026-04-31", "2026-13-01", "2026-00-01",
            "2026-01-00", "0000-01-01").forEach { date ->
            assertNull(date, markDateToCreatedTime(date))
        }
    }

    @Test
    fun markDateRejectsIncompleteOrMalformedInput() {
        listOf("", " ", "2026-9-1", "26-09-14", "2026/09/14", "2026-09-14x",
            "2026-09-14T12:00:00Z", "2026-09-140").forEach { date ->
            assertNull(date, markDateToCreatedTime(date))
        }
    }

    @Test
    fun markDateFormattingDoesNotDependOnDeviceCalendarOrDigits() {
        val locale = Locale.getDefault()
        val timeZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale("th", "TH"))
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"))

            assertEquals("1970-01-01", currentMarkDate(Date(0)))
            assertEquals("2024-02-29T00:00:00Z", markDateToCreatedTime("2024-02-29"))
        } finally {
            Locale.setDefault(locale)
            TimeZone.setDefault(timeZone)
        }
    }

    @Test
    fun markRequestOmitsUnspecifiedDateAndRating() {
        val request = MarkInRequest(shelfType = "complete", visibility = 0)
        val json = Gson().toJsonTree(request).asJsonObject

        assertFalse(json.has("created_time"))
        assertFalse(json.has("rating_grade"))
    }

    @Test
    fun markRequestIncludesSpecifiedDate() {
        val request = MarkInRequest(
            shelfType = "complete",
            visibility = 0,
            createdTime = markDateToCreatedTime("2024-02-29")
        )
        val json = Gson().toJsonTree(request).asJsonObject

        assertEquals("2024-02-29T00:00:00Z", json.get("created_time").asString)
    }

    private fun input(text: String) = TextFieldValue(text, selection = TextRange(text.length))
}
