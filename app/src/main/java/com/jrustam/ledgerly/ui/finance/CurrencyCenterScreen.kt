package com.jrustam.ledgerly.ui.finance

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.model.CurrencyConversion
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.ui.common.*

@Composable fun CurrencyCenterScreen(vm: ProductViewModel, default: LedgerCurrency) {
    var base by rememberSaveable { mutableStateOf(default.name) }; var target by rememberSaveable { mutableStateOf(if (default == LedgerCurrency.EUR) "USD" else "EUR") }
    var amount by rememberSaveable { mutableStateOf("1") }
    val quote by vm.quote.collectAsStateWithLifecycle(); val busy by vm.rateBusy.collectAsStateWithLifecycle(); val failed by vm.rateError.collectAsStateWithLifecycle()
    LaunchedEffect(base,target) { vm.loadRate(base,target) }
    FinancePage {
        Text(stringResource(R.string.rate_privacy))
        CurrencySelector(LedgerCurrency.valueOf(base),!busy,onSelect = { base = it.name })
        CurrencySelector(LedgerCurrency.valueOf(target),!busy,onSelect = { target = it.name })
        Field(amount,{ amount = it },R.string.target_amount,true)
        Button({ vm.loadRate(base,target,true) },enabled = !busy && base != target) { Text(stringResource(R.string.refresh_rates)) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (failed) Text(stringResource(R.string.rate_failed),color = MaterialTheme.colorScheme.error)
        val conversion = vm.convert(amount, base, target, quote)
        if (conversion is CurrencyConversion.Exact) Text(formatMoney(conversion.minor,target,appLocale()))
        else if (conversion is CurrencyConversion.Approximate) {
            val current = conversion.quote
            Text(stringResource(R.string.approximate_rate),style = MaterialTheme.typography.titleMedium)
            Text(formatMoney(conversion.minor,target,appLocale()),style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.rate_timestamp,java.time.LocalDate.parse(current.rateDate).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(appLocale())),java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT,appLocale()).format(java.util.Date(current.fetchedAtMillis))))
        } else Text(stringResource(R.string.rate_unavailable))
    }
}
