package com.jrustam.ledgerly.ui.common

import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.model.Category
import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.jrustam.ledgerly.domain.model.AppLanguage
import com.jrustam.ledgerly.domain.model.LedgerCurrency

fun formatMoney(minor: BigInteger, currencyCode: String, locale: Locale): String =
    NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance(currencyCode)
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal(minor, 2))

fun formatMoney(minor: Long, currencyCode: String, locale: Locale): String =
    formatMoney(BigInteger.valueOf(minor), currencyCode, locale)

@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

fun AppLanguage.labelResource(): Int = when (this) {
    AppLanguage.ARABIC -> R.string.language_ar
    AppLanguage.FRENCH -> R.string.language_fr
    AppLanguage.GERMAN -> R.string.language_de
    AppLanguage.UKRAINIAN -> R.string.language_uk
    AppLanguage.SERBIAN_LATIN -> R.string.language_sr_latn
    AppLanguage.SERBIAN_CYRILLIC -> R.string.language_sr_cyrl
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.AZERBAIJANI -> R.string.language_az
    AppLanguage.ENGLISH -> R.string.language_en
    AppLanguage.RUSSIAN -> R.string.language_ru
    AppLanguage.TURKISH -> R.string.language_tr
}

fun LedgerCurrency.labelResource(): Int = when (this) {
    LedgerCurrency.UAH -> R.string.currency_uah
    LedgerCurrency.RSD -> R.string.currency_rsd
    LedgerCurrency.AZN -> R.string.currency_azn
    LedgerCurrency.USD -> R.string.currency_usd
    LedgerCurrency.EUR -> R.string.currency_eur
    LedgerCurrency.RUB -> R.string.currency_rub
}

fun Category.labelResource(): Int = when (this) {
    Category.FOOD -> R.string.category_food
    Category.TRANSPORT -> R.string.category_transport
    Category.SHOPPING -> R.string.category_shopping
    Category.BILLS -> R.string.category_bills
    Category.HEALTH -> R.string.category_health
    Category.OTHER_EXPENSE, Category.OTHER_INCOME -> R.string.category_other
    Category.SALARY -> R.string.category_salary
    Category.FREELANCE -> R.string.category_freelance
    Category.GIFT -> R.string.category_gift
}
