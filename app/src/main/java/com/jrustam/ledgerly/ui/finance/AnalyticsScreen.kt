package com.jrustam.ledgerly.ui.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.calculation.progressFraction
import com.jrustam.ledgerly.domain.model.BalanceSummary
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.ui.common.*
import com.jrustam.ledgerly.ui.design.*
import java.math.BigInteger
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun MonthSummary(totals: BalanceSummary, currency: String, month: YearMonth) {
    LedgerCard { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",appLocale())), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.monthly_cash_flow), color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedMoney(formatMoney(totals.balanceMinor,currency,appLocale()), MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.income) + " · " + formatMoney(totals.incomeMinor,currency,appLocale()))
        Text(stringResource(R.string.expense) + " · " + formatMoney(totals.expenseMinor,currency,appLocale()))
    } }
}
@Composable
fun AnalyticsScreen(state: FinanceState, default: LedgerCurrency, navigate: (String) -> Unit, onMonth: (Int) -> Unit) {
    var currency by rememberSaveable { mutableStateOf(default.name) }
    val offset = state.monthOffset
    val report = state.reports[currency] ?: return
    val month = report.month
    val totals = report.totals
    val prior = report.previousTotals
    FinancePage {
        Text(stringResource(R.string.monthly_recap), style = MaterialTheme.typography.headlineMedium)
        CurrencySelector(LedgerCurrency.valueOf(currency), true, onSelect = { currency = it.name })
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton({ onMonth(offset - 1) }) { Text(stringResource(R.string.previous_month)) }
            TextButton({ onMonth(offset + 1) }, enabled = offset < 0) { Text(stringResource(R.string.next_month)) }
        }
        MonthSummary(totals,currency,month)
        TextButton({ navigate("currency_center") }) { Text(stringResource(R.string.currency_center)) }
        if (!report.hasTransactions) Text(stringResource(R.string.empty_history_title))
        else {
            Text(stringResource(R.string.category_breakdown), style = MaterialTheme.typography.titleMedium)
            val categories = report.expensesByCategory
            categories.forEach { (category, amount) ->
                Text(stringResource(category.labelResource()) + " · " + formatMoney(amount,currency,appLocale()))
                LinearProgressIndicator(progress = { progressFraction(amount,totals.expenseMinor) }, modifier = Modifier.fillMaxWidth())
            }
        }
        Text(stringResource(R.string.monthly_trend), style = MaterialTheme.typography.titleMedium)
        val maximum = report.trend.maxOfOrNull { it.amountMinor } ?: BigInteger.ZERO
        report.trend.forEach { (period, amount) ->
            Text(period.format(DateTimeFormatter.ofPattern("MMM yyyy",appLocale())) + " · " + formatMoney(amount,currency,appLocale()))
            LinearProgressIndicator(progress = { progressFraction(amount,maximum) }, modifier = Modifier.fillMaxWidth())
        }
        Text(stringResource(R.string.smart_insights), style = MaterialTheme.typography.titleMedium)
        if (totals.expenseMinor != BigInteger.ZERO || prior.expenseMinor != BigInteger.ZERO) {
            Text(stringResource(if (totals.expenseMinor > prior.expenseMinor) R.string.spending_higher else R.string.spending_lower,
                formatMoney((totals.expenseMinor - prior.expenseMinor).abs(),currency,appLocale())))
            Text(stringResource(R.string.comparison_hint), style = MaterialTheme.typography.bodySmall)
        }
        report.budgetWarnings.forEach { name ->
            Text(name + " · " + stringResource(R.string.budget_warning), color = MaterialTheme.colorScheme.error)
        }
        Text(stringResource(R.string.recurring_total,formatMoney(report.recurringMinor,currency,appLocale())))
    }
}
