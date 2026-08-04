package com.krelinnbios.neodblite.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
