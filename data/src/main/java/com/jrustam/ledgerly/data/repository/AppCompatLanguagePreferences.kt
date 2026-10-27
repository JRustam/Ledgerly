package com.jrustam.ledgerly.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.jrustam.ledgerly.domain.repository.LanguagePreferences
import com.jrustam.ledgerly.domain.model.AppLanguage
import javax.inject.Inject

/** AppCompat persists locales on API 24–32; the framework persists them on API 33+. */
class AppCompatLanguagePreferences @Inject constructor() : LanguagePreferences {
    override fun selectedLanguage(): AppLanguage =
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(','))
    override fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
