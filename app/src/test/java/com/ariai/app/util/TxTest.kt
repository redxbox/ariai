package com.ariai.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TxTest {
    @Test
    fun providerFailureMessagesHavePersianTranslationsAndEnglishFallback() {
        val originalLanguage = UiLang.code
        try {
            UiLang.code = "fa"
            assertEquals("دریافت مدل‌ها ناموفق بود", tx("Could not load models"))
            assertEquals("ذخیرهٔ ارائه‌دهنده انجام نشد. دوباره تلاش کنید.", tx("Could not save provider. Please try again."))

            UiLang.code = "en"
            assertEquals("Could not save provider. Please try again.", tx("Could not save provider. Please try again."))
        } finally {
            UiLang.code = originalLanguage
        }
    }

    @Test
    fun configuredLanguageLabelsAndSearchDescriptionsAreTranslated() {
        val originalLanguage = UiLang.code
        try {
            UiLang.code = "fa"
            assertEquals("فارسی", tx("Persian"))
            assertNotEquals("Research-focused search API", tx("Research-focused search API"))
        } finally {
            UiLang.code = originalLanguage
        }
    }
}
