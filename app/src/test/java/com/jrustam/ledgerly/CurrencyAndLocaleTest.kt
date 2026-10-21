package com.jrustam.ledgerly

import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.common.formatMoney
import java.math.BigInteger
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class CurrencyAndLocaleTest {
    private fun row(code: String, amount: Long, type: TransactionType = TransactionType.INCOME) = Transaction(
        code + type, amount, code, type, if (type == TransactionType.INCOME) Category.SALARY else Category.FOOD,
        LocalDate.of(2026, 1, 1), null)
    @Test fun balancesNeverCombineDifferentCurrencies() {
        val totals = calculateBalances(listOf(row("USD", 10000), row("AZN", 20000), row("EUR", 500),
            row("RUB", 10), row("AZN", 1000, TransactionType.EXPENSE)))
        assertEquals(setOf("AZN", "EUR", "RUB", "USD"), totals.keys)
        assertEquals(BigInteger("19000"), totals.getValue("AZN").balanceMinor)
        assertEquals(BigInteger("10000"), totals.getValue("USD").balanceMinor)
        assertEquals(BigInteger("500"), totals.getValue("EUR").balanceMinor)
        assertEquals(BigInteger.TEN, totals.getValue("RUB").balanceMinor)
    }
    @Test fun formattingRetainsExactCentsAcrossEveryLocaleAndCurrency() {
        for (language in listOf("az", "en", "ru", "tr")) {
            for (currency in LedgerCurrency.entries) {
                val locale = Locale.forLanguageTag(language)
                val minor = BigInteger("19999999999999999980")
                val text = formatMoney(minor, currency.name, locale)
                val parser = (NumberFormat.getCurrencyInstance(locale) as DecimalFormat).apply {
                    this.currency = java.util.Currency.getInstance(currency.name)
                    isParseBigDecimal = true
                }
                val position = ParsePosition(0)
                assertEquals(BigDecimal(minor, 2), parser.parse(text, position))
                assertEquals(text.length, position.index)
            }
        }
    }
    @Test fun formattingUsesLocaleSeparatorsAndHandlesNegativeBalance() {
        val english = formatMoney(-123456L, "USD", Locale.ENGLISH)
        val turkish = formatMoney(-123456L, "USD", Locale.forLanguageTag("tr"))
        assertTrue(english.contains("1,234.56"))
        assertTrue(turkish.contains("1.234,56"))
        assertTrue(english.contains("-"))
    }
    @Test fun languageSelectionHandlesSystemAndRegionalTags() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.AZERBAIJANI, AppLanguage.fromTag("az-AZ"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromTag("ru"))
        assertEquals(AppLanguage.TURKISH, AppLanguage.fromTag("tr-TR"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("unknown"))
    }
}
