package io.github.zxl13939888272hub.yexupdf.data.repository

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingManagerLocaleTest {

    @Test
    fun `simplified Chinese system locale selects Chinese UI`() {
        assertEquals("zh-CN", OnboardingManager.defaultLocaleTag(Locale.forLanguageTag("zh-CN")))
    }

    @Test
    fun `other Chinese regions still use available simplified Chinese UI`() {
        assertEquals("zh-CN", OnboardingManager.defaultLocaleTag(Locale.forLanguageTag("zh-HK")))
        assertEquals("zh-CN", OnboardingManager.defaultLocaleTag(Locale.forLanguageTag("zh-TW")))
    }

    @Test
    fun `unsupported system locale falls back to English`() {
        assertEquals("en", OnboardingManager.defaultLocaleTag(Locale.forLanguageTag("de-DE")))
    }
}
