package com.jrustam.ledgerly.domain.model

enum class AppLanguage(val tag: String) {
    SYSTEM(""), AZERBAIJANI("az"), ENGLISH("en"), RUSSIAN("ru"), TURKISH("tr"), UKRAINIAN("uk"), SERBIAN_LATIN("sr-Latn"), SERBIAN_CYRILLIC("sr-Cyrl"), FRENCH("fr"), GERMAN("de"), ARABIC("ar");
    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag.equals(tag, true) } ?: if (tag.startsWith("sr", ignoreCase = true)) {
            if (tag.split('-').any { it.equals("Latn", true) }) SERBIAN_LATIN else SERBIAN_CYRILLIC
        } else entries.firstOrNull { it.tag == tag.substringBefore('-') } ?: SYSTEM
    }
}
