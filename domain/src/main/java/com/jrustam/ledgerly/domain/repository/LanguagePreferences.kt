package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.AppLanguage

interface LanguagePreferences {
    fun selectedLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}
