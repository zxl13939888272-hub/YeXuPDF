package io.github.zxl13939888272hub.yexupdf.imageeditor.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class U2NetMaskPostProcessorTest {
    @Test
    fun mapsFiniteRangeToFullAlphaRange() {
        val alpha = U2NetMaskPostProcessor.alpha(floatArrayOf(-2f, -1f, 0f, 1f, 2f))
        assertEquals(0, alpha.first().toInt() and 0xFF)
        assertEquals(255, alpha.last().toInt() and 0xFF)
        assertEquals(128, alpha[2].toInt() and 0xFF)
    }

    @Test
    fun flatOrInvalidMaskBecomesTransparent() {
        assertArrayEquals(ByteArray(3), U2NetMaskPostProcessor.alpha(floatArrayOf(4f, 4f, 4f)))
        assertArrayEquals(ByteArray(2), U2NetMaskPostProcessor.alpha(floatArrayOf(Float.NaN, Float.NaN)))
    }
}
