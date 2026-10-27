package com.jrustam.ledgerly

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class AppLanguageTest {
    @Test fun selectedLanguageSurvivesActivityRecreationAndUsesTranslatedResources() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var original = ""
        scenario.onActivity { original = AppCompatDelegate.getApplicationLocales().toLanguageTags() }
        try {
            for ((tag, settings) in mapOf("az" to "Ayarlar", "en" to "Settings", "ru" to "Настройки", "tr" to "Ayarlar", "uk" to "Налаштування", "sr-Latn" to "Podešavanja", "sr-Cyrl" to "Подешавања")) {
                scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) }
                instrumentation.waitForIdleSync()
                scenario.recreate()
                scenario.onActivity {
                    assertEquals(tag, AppCompatDelegate.getApplicationLocales().toLanguageTags())
                    assertEquals(settings, it.getString(R.string.settings))
                    assertEquals(tag.substringBefore('-'), it.resources.configuration.locales[0].language)
                }
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(original)) }
            instrumentation.waitForIdleSync()
            scenario.close()
        }
    }
}
