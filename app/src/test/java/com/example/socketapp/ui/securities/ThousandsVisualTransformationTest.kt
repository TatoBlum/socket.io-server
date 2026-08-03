package com.example.socketapp.ui.securities

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsVisualTransformationTest {

    private val transformation = ThousandsVisualTransformation()

    @Test
    fun `formats integer part with thousands separators`() {
        assertFormatted("", "")
        assertFormatted("1", "1")
        assertFormatted("12", "12")
        assertFormatted("123", "123")
        assertFormatted("1234", "1.234")
        assertFormatted("12345", "12.345")
        assertFormatted("1234567", "1.234.567")
    }

    @Test
    fun `keeps decimal separator and decimal digits`() {
        assertFormatted("1234,", "1.234,")
        assertFormatted("1234,5", "1.234,5")
        assertFormatted("1234,567", "1.234,567")
    }

    @Test
    fun `keeps cursor at end for one and two digits`() {
        assertEndCursor(raw = "1", transformed = "1")
        assertEndCursor(raw = "12", transformed = "12")
    }

    @Test
    fun `offset mapping round trips original offsets`() {
        val raw = "1234567,890"
        val result = transformation.filter(AnnotatedString(raw))

        for (offset in 0..raw.length) {
            val transformedOffset = result.offsetMapping.originalToTransformed(offset)
            val originalOffset = result.offsetMapping.transformedToOriginal(transformedOffset)

            assertEquals(offset, originalOffset)
        }
    }

    private fun assertFormatted(raw: String, expected: String) {
        val result = transformation.filter(AnnotatedString(raw))

        assertEquals(expected, result.text.text)
    }

    private fun assertEndCursor(raw: String, transformed: String) {
        val result = transformation.filter(AnnotatedString(raw))

        assertEquals(transformed, result.text.text)
        assertEquals(transformed.length, result.offsetMapping.originalToTransformed(raw.length))
        assertEquals(raw.length, result.offsetMapping.transformedToOriginal(transformed.length))
    }
}
