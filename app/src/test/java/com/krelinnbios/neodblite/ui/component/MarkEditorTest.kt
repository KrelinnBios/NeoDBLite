package com.krelinnbios.neodblite.ui.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.gson.Gson
import com.krelinnbios.neodblite.data.model.MarkInRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        listOf("", "read ", "read  ").forEach { text ->
            val result = completeMarkTag(input(text), "science")
            val expected = if (text.isEmpty()) "science " else "read science "
            assertEquals(expected, result.text)
            assertEquals(TextRange(result.text.length), result.selection)
        }
    }

    @Test
    fun tagParsingOnlyUsesWhitespaceAsSeparator() {
        assertEquals(listOf("read,science", "已读，科幻、漫画\t标签"), parseMarkTags("read,science 已读，科幻、漫画\t标签"))
        assertEquals("read,sci", markTagQuery(input("read,sci")))
        assertEquals("science ", completeMarkTag(input("read,sci"), "science").text)
        assertEquals("标签\t片段", markTagQuery(input("标签\t片段")))
    }

    @Test
    fun tagInputRejectsCommaCharacters() {
        assertTrue(hasInvalidMarkTagInput("read,science"))
        assertTrue(hasInvalidMarkTagInput("已读，科幻、漫画"))
        assertFalse(hasInvalidMarkTagInput("已读  科幻"))
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
    fun markDateInputAddsSeparatorsAsSoonAsYearAndMonthAreComplete() {
        val expected = listOf("", "2", "20", "202", "2020-", "2020-0", "2020-09-", "2020-09-1", "2020-09-14")
        var value = input("")
        expected.forEachIndexed { length, formatted ->
            value = updateMarkDateInput(value, input("20200914".take(length)))
            val transformed = markDateVisualTransformation.filter(AnnotatedString(value.text))

            assertEquals(formatted, transformed.text.text)
            assertEquals(formatted.length, transformed.offsetMapping.originalToTransformed(value.selection.end))
        }
    }

    @Test
    fun markDateInputAcceptsPastedDateAndPreservesSelection() {
        val value = TextFieldValue("2020-09-14", selection = TextRange(7, 5))
        val result = updateMarkDateInput(input(""), value)

        assertEquals("20200914", result.text)
        assertEquals(TextRange(6, 4), result.selection)
        assertEquals("20200914", updateMarkDateInput(input(""), input(" 2020-09-14 ")).text)
    }

    @Test
    fun markDateInputPreservesCursorWhenEditingInMiddle() {
        val previous = input("20200914")
        val value = TextFieldValue("20200814", selection = TextRange(6), composition = TextRange(4, 6))

        assertEquals(value, updateMarkDateInput(previous, value))
        assertEquals("2020-08-14", formatMarkDateInput(value.text))
    }

    @Test
    fun markDateInputRejectsUnexpectedCharacters() {
        val previous = input("2020")

        listOf("2020x", "2020/09/14").forEach { text ->
            assertEquals(previous, updateMarkDateInput(previous, input(text)))
        }
    }

    @Test
    fun markDateInputKeepsExtraDigitsVisibleAndWarns() {
        val result = updateMarkDateInput(input("20200914"), input("202009149"))

        assertEquals("202009149", result.text)
        assertEquals("2020-09-149", formatMarkDateInput(result.text))
        assertTrue(markDateInputHasWarning(result.text, today = "2020-09-14"))
    }

    @Test
    fun markDateInputWarnsForFutureAndInvalidDates() {
        listOf("20200915", "20201001", "20210101", "20200230", "20201301").forEach { digits ->
            assertTrue(digits, markDateInputHasWarning(digits, today = "2020-09-14"))
        }
    }

    @Test
    fun markDateInputAllowsTodayPastDatesAndIncompleteInput() {
        listOf("20200914", "20200913", "20191231", "20200229", "2020", "202009", "").forEach { digits ->
            assertFalse(digits, markDateInputHasWarning(digits, today = "2020-09-14"))
        }
    }

    @Test
    fun markDateInputSupportsBackspacingAcrossBothSeparators() {
        listOf("2020" to "202", "202009" to "2020-0").forEach { (digits, expected) ->
            val previous = input(digits)
            val transformed = markDateVisualTransformation.filter(AnnotatedString(digits))
            val cursor = transformed.offsetMapping.transformedToOriginal(transformed.text.length)
            val edited = TextFieldValue(digits.removeRange(cursor - 1, cursor), selection = TextRange(cursor - 1))
            val result = updateMarkDateInput(previous, edited)

            assertEquals(expected, formatMarkDateInput(result.text))
        }
        assertEquals("", updateMarkDateInput(input("20200914"), input("")).text)
    }

    @Test
    fun markDateInputCursorMappingsStayValidAtEveryInputLength() {
        for (length in 0..12) {
            val transformed = markDateVisualTransformation.filter(AnnotatedString("202009149999".take(length)))
            val mapping = transformed.offsetMapping
            for (offset in 0..length) {
                val displayedOffset = mapping.originalToTransformed(offset)
                assertTrue(displayedOffset in 0..transformed.text.length)
                assertEquals(offset, mapping.transformedToOriginal(displayedOffset))
            }
            val digitOffsets = (0..transformed.text.length).map(mapping::transformedToOriginal)
            assertTrue(digitOffsets.all { it in 0..length })
            assertEquals(digitOffsets.sorted(), digitOffsets)
        }
    }

    @Test
    fun autoFormattedMarkDateStillRequiresACompleteValidDate() {
        assertEquals("2020-02-29T00:00:00Z", markDateToCreatedTime(formatMarkDateInput("20200229")))
        listOf("2020", "202009", "2020091", "20200230").forEach { digits ->
            assertNull(markDateToCreatedTime(formatMarkDateInput(digits)))
        }
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
