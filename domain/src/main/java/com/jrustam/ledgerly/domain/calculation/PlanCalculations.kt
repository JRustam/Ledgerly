package com.jrustam.ledgerly.domain.calculation

import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun budgetSpent(plan: FinancePlan, transactions: List<Transaction>): BigInteger = transactions
    .filter {
        it.currency == plan.currency && it.type == TransactionType.EXPENSE && it.category == plan.category && YearMonth.from(
            it.date
        ) == YearMonth.from(plan.date)
    }
    .fold(BigInteger.ZERO) { sum, t -> sum + BigInteger.valueOf(t.amountMinor) }

fun goalSaved(plan: FinancePlan, contributions: List<Contribution>): BigInteger = contributions
    .filter { it.goalId == plan.id }
    .fold(BigInteger.ZERO) { sum, c -> sum + BigInteger.valueOf(c.amountMinor) }

/** Forecast from observed contributions; absent until there is a non-zero observation window. */
fun goalEstimate(
    plan: FinancePlan,
    contributions: List<Contribution>,
    today: LocalDate
): LocalDate? {
    val rows = contributions.filter { it.goalId == plan.id && !it.date.isAfter(today) }
    val saved = goalSaved(plan, rows)
    val remaining = BigInteger.valueOf(plan.amountMinor) - saved
    if (remaining.signum() <= 0) return today
    val days =
        rows.minOfOrNull { it.date }?.let { ChronoUnit.DAYS.between(it, today) } ?: return null
    if (days <= 0 || saved.signum() <= 0) return null
    val needed = BigDecimal(remaining).multiply(BigDecimal.valueOf(days))
        .divide(BigDecimal(saved), 0, RoundingMode.CEILING)
    return runCatching { today.plusDays(needed.longValueExact()) }.getOrNull()
}

/** Float is used only for drawing a bounded progress indicator, never money. */
fun progressFraction(value: BigInteger, total: BigInteger): Float = if (total.signum() <= 0) 0f else
    BigDecimal(value).divide(BigDecimal(total), 6, RoundingMode.HALF_UP)
        .coerceIn(BigDecimal.ZERO, BigDecimal.ONE).toFloat()

/** Clamp month-end dates while preserving the original schedule's day. */
fun upcomingPayments(start: LocalDate, today: LocalDate, count: Int = 6): List<LocalDate> {
    require(count in 0..120)
    var month = maxOf(YearMonth.from(start), YearMonth.from(today))
    val dates = mutableListOf<LocalDate>()
    while (dates.size < count) {
        val due = month.atDay(minOf(start.dayOfMonth, month.lengthOfMonth()))
        if (!due.isBefore(start) && !due.isBefore(today)) dates += due
        month = month.plusMonths(1)
    }
    return dates
}
