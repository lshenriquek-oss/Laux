package com.codeoneditor.core.editor.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.dp
import org.junit.Test

class EditorLayoutMetricsTest {
    @Test
    fun gutterWidthHasMinimumSize() {
        val width = calculateLineNumberGutterWidth(36.dp)
        assertEquals(width, calculateLineNumberGutterWidth(36.dp))
        assertTrue(width.value >= 16f)
    }

    @Test
    fun gutterKeepsCodeCloseToLineNumbers() {
        val oneDigit = calculateLineNumberGutterWidth(9.dp)
        val threeDigits = calculateLineNumberGutterWidth(27.dp)

        // The gutter has only a small fixed right gap. It must not use the
        // previous oversized 48.dp minimum, which pushed the code too far away.
        assertEquals(16f, oneDigit.value, 0.001f)
        assertEquals(35f, threeDigits.value, 0.001f)
        assertTrue(threeDigits >= oneDigit)
    }

    @Test
    fun lineNumberBaselineFollowsLinePositionAndScroll() {
        val baseline = calculateLineNumberBaselineY(
            lineIndex = 9,
            lineHeightPx = 80f,
            scrollOffsetPx = 150f,
            baselineOffsetPx = 20f
        )
        assertEquals(590f, baseline, 0.001f)
    }

    @Test
    fun lineNumberBaselineUsesTheZoomedLineHeight() {
        val normal = calculateLineNumberBaselineY(
            lineIndex = 4,
            lineHeightPx = 20f,
            scrollOffsetPx = 0f,
            baselineOffsetPx = 15f
        )
        val zoomed = calculateLineNumberBaselineY(
            lineIndex = 4,
            lineHeightPx = 80f,
            scrollOffsetPx = 0f,
            baselineOffsetPx = 15f
        )
        assertEquals(95f, normal, 0.001f)
        assertEquals(335f, zoomed, 0.001f)
    }

    @Test
    fun lineNumberFontSizeScalesWithEditorZoom() {
        assertEquals(7f, calculateLineNumberFontSizeSp(0.5f), 0.001f)
        assertEquals(14f, calculateLineNumberFontSizeSp(1f), 0.001f)
        assertEquals(28f, calculateLineNumberFontSizeSp(2f), 0.001f)
        assertEquals(56f, calculateLineNumberFontSizeSp(4f), 0.001f)
    }

    @Test
    fun lineNumberHorizontalAnchorStaysFixedWhenZoomChanges() {
        val anchor = calculateLineNumberTextLeftPadding()
        assertEquals(4f, anchor.value, 0.001f)
        assertEquals(anchor, calculateLineNumberTextLeftPadding())
    }

    @Test
    fun zoomIsClampedToSafeEditorRange() {
        assertEquals(0.50f, clampEditorZoom(0.01f), 0.0001f)
        assertEquals(4.00f, clampEditorZoom(20f), 0.0001f)
        assertEquals(2.00f, clampEditorZoom(2f), 0.0001f)
    }
}
