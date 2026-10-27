package com.jrustam.ledgerly.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.calculation.budgetSpent
import com.jrustam.ledgerly.domain.calculation.goalEstimate
import com.jrustam.ledgerly.domain.calculation.goalSaved
import com.jrustam.ledgerly.domain.calculation.progressFraction
import com.jrustam.ledgerly.domain.calculation.upcomingPayments
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.usecase.AddTransaction
import com.jrustam.ledgerly.ui.common.*
import com.jrustam.ledgerly.ui.design.*
import com.jrustam.ledgerly.ui.overview.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Composable
fun FinancePage(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable
fun Field(value: String, onValue: (String) -> Unit, label: Int, money: Boolean = false) {
    OutlinedTextField(value, onValue, label = { Text(stringResource(label)) }, modifier = Modifier.fillMaxWidth(),
        singleLine = true, shape = LedgerShapes.control,
        keyboardOptions = KeyboardOptions(keyboardType = if (money) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable
fun Choice(label: String, values: List<String>, selected: Int, onSelect: (Int) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(label + ": " + values.getOrElse(selected) { "" })
        }
        DropdownMenu(open, { open = false }) {
            values.forEachIndexed { i, value -> DropdownMenuItem(text = { Text(value) }, onClick = { open = false; onSelect(i) }) }
        }
    }
}
@Composable
fun WalletChoice(wallets: List<Wallet>, selected: String?, onSelect: (Wallet?) -> Unit, enabled: Boolean = true) {
    Choice(stringResource(R.string.wallet), listOf(stringResource(R.string.unassigned)) + wallets.map { "${it.name} · ${it.currency}" },
        wallets.indexOfFirst { it.id == selected } + 1, { onSelect(wallets.getOrNull(it - 1)) }, enabled)
}
@Composable
fun WalletsScreen(state: FinanceState, navigate: (String) -> Unit) {
    val locale = appLocale()
    FinancePage {
        Text(stringResource(R.string.wallets_intro), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ navigate("wallet/new") }) { Text(stringResource(R.string.add_wallet)) }
            OutlinedButton({ navigate("transfer") }, enabled = state.data.wallets.size >= 2) { Text(stringResource(R.string.transfer)) }
        }
        TextButton({ navigate("connect") }) { Text(stringResource(R.string.connect_bank)) }
        if (state.data.wallets.isEmpty()) EmptyPanel(stringResource(R.string.wallets), stringResource(R.string.wallet_empty))
        state.data.wallets.forEach { wallet ->
            WalletCard(wallet, state.balances.wallets.getValue(wallet.id)) { navigate("wallet_details/${wallet.id}") }
        }
        if (state.transactions.any { it.walletId == null }) {
            LedgerCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.unassigned), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.legacy_wallet_hint))
                state.balances.unassigned.forEach { (currency, totals) ->
                    Text(formatMoney(totals.balanceMinor, currency, locale))
                }
            } }
        }
    }
}
private val walletTones = listOf(Color(0xFF174C44), Color(0xFF243C63), Color(0xFF503D60))
@Composable
fun WalletCard(wallet: Wallet, balance: BigInteger, onClick: () -> Unit) {
    val tone = walletTones[wallet.theme.coerceIn(0, 2)]
    Surface(shape = LedgerShapes.card, color = tone, contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.background(Brush.linearGradient(listOf(tone, tone.copy(alpha = .7f))))
            .clickable(onClick = onClick).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(wallet.name, style = MaterialTheme.typography.titleLarge)
            Text(wallet.bank.ifBlank { stringResource(R.string.manually_managed) }, style = MaterialTheme.typography.labelMedium)
            AnimatedMoney(formatMoney(balance, wallet.currency, appLocale()), MaterialTheme.typography.headlineLarge, Color.White)
            Text(wallet.currency + if (wallet.lastFour.isNotBlank()) "  •••• ${wallet.lastFour}" else "")
        }
    }
}
@Composable
fun WalletForm(existing: Wallet?, currency: LedgerCurrency, busy: Boolean, save: (Wallet) -> Unit) {
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var bank by rememberSaveable { mutableStateOf(existing?.bank.orEmpty()) }
    var last by rememberSaveable { mutableStateOf(existing?.lastFour.orEmpty()) }
    var opening by rememberSaveable { mutableStateOf(existing?.let { BigDecimal.valueOf(it.openingMinor, 2).toPlainString() } ?: "0") }
    var selected by rememberSaveable { mutableStateOf(existing?.currency ?: currency.name) }
    var kind by rememberSaveable { mutableStateOf(existing?.kind ?: "CASH") }
    var theme by rememberSaveable { mutableIntStateOf(existing?.theme ?: 0) }
    val minor = if (Regex("-?[0-9]{1,16}([.,][0-9]{1,2})?").matches(opening)) runCatching { BigDecimal(opening.replace(',', '.')).movePointRight(2).longValueExact() }.getOrNull() else null
    val valid = name.isNotBlank() && name.length <= 60 && bank.length <= 60 && minor != null && (last.isEmpty() || Regex("[0-9]{4}").matches(last))
    FinancePage {
        Text(stringResource(R.string.manually_managed), color = MaterialTheme.colorScheme.primary)
        Field(name, { name = it }, R.string.display_name)
        Field(bank, { bank = it }, R.string.bank_name)
        Choice(stringResource(R.string.account_type), listOf(stringResource(R.string.cash), stringResource(R.string.card), stringResource(R.string.bank)),
            listOf("CASH", "CARD", "BANK").indexOf(kind), { kind = listOf("CASH", "CARD", "BANK")[it] })
        CurrencySelector(LedgerCurrency.valueOf(selected), existing == null && !busy, onSelect = { selected = it.name })
        Text(stringResource(R.string.wallet_currency_fixed), style = MaterialTheme.typography.bodySmall)
        Field(opening, { opening = it }, R.string.opening_balance, true)
        Text(stringResource(R.string.adjustment_hint), style = MaterialTheme.typography.bodySmall)
        Field(last, { if (it.length <= 4 && it.all(Char::isDigit)) last = it }, R.string.last_four)
        Choice(stringResource(R.string.card_theme), listOf(stringResource(R.string.emerald), stringResource(R.string.ocean), stringResource(R.string.plum)), theme, { theme = it })
        Button({ save(Wallet(id,name,bank,kind,selected,minor!!,theme,last)) }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_changes)) }
    }
}
@Composable
fun WalletDetails(wallet: Wallet, state: FinanceState, navigate: (String) -> Unit, deleteTransfer: (String) -> Unit) {
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    FinancePage {
        WalletCard(wallet, state.balances.wallets.getValue(wallet.id)) { navigate("wallet/${wallet.id}") }
        TextButton({ navigate("wallet/${wallet.id}") }) { Text(stringResource(R.string.edit)) }
        Text(stringResource(R.string.transactions), style = MaterialTheme.typography.titleMedium)
        val rows = state.transactions.filter { it.walletId == wallet.id }
        if (rows.isEmpty()) Text(stringResource(R.string.empty_history_title))
        rows.forEach { TransactionCard(it, { navigate("transaction/${it.id}") }) }
        Text(stringResource(R.string.transfers), style = MaterialTheme.typography.titleMedium)
        state.data.transfers.filter { it.fromId == wallet.id || it.toId == wallet.id }.forEach { transfer ->
            val incoming = transfer.toId == wallet.id
            LedgerCard { Column(Modifier.padding(16.dp)) {
                Text((if (incoming) "+" else "−") + formatMoney(if (incoming) transfer.creditMinor else transfer.debitMinor, wallet.currency, appLocale()), style = MaterialTheme.typography.titleMedium)
                Text(state.data.wallets.firstOrNull { it.id == (if (incoming) transfer.fromId else transfer.toId) }?.name.orEmpty())
                Text(transfer.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale())))
                TextButton({ pending = transfer.id }) { Text(stringResource(R.string.delete)) }
            } }
        }
    }
    if (pending != null) ConfirmDelete({ pending = null }) { deleteTransfer(pending!!); pending = null }
}
@Composable
fun TransferForm(wallets: List<Wallet>, busy: Boolean, save: (Transfer) -> Unit) {
    var from by rememberSaveable { mutableStateOf(wallets.firstOrNull()?.id) }
    var to by rememberSaveable { mutableStateOf(wallets.getOrNull(1)?.id) }
    var debit by rememberSaveable { mutableStateOf("") }; var credit by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val id = rememberSaveable { UUID.randomUUID().toString() }
    val source = wallets.firstOrNull { it.id == from }; val target = wallets.firstOrNull { it.id == to }
    val same = source?.currency == target?.currency
    val d = AddTransaction.parseAmount(debit); val c = if (same) d else AddTransaction.parseAmount(credit)
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    FinancePage {
        Text(stringResource(R.string.transfer_hint))
        Text(stringResource(R.string.from_wallet)); WalletChoice(wallets, from, { from = it?.id })
        Text(stringResource(R.string.to_wallet)); WalletChoice(wallets, to, { to = it?.id })
        Field(debit, { debit = it }, R.string.debit_amount, true)
        if (!same) { Field(credit, { credit = it }, R.string.credit_amount, true); Text(stringResource(R.string.transfer_fx_hint)) }
        Field(date, { date = it }, R.string.iso_date)
        Button({ save(Transfer(id,from!!,to!!,d!!,c!!,day!!)) }, enabled = !busy && source != null && target != null && from != to && d != null && c != null && day != null,
            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.transfer)) }
    }
}
fun PlanKind.labelResource() = when(this) { PlanKind.BUDGET -> R.string.budget; PlanKind.GOAL -> R.string.savings_goal; PlanKind.RECURRING -> R.string.recurring }
@Composable
fun PlansScreen(state: FinanceState, navigate: (String) -> Unit) {
    var kind by rememberSaveable { mutableStateOf(PlanKind.BUDGET) }
    FinancePage {
        Text(stringResource(R.string.plans_intro), style = MaterialTheme.typography.headlineMedium)
        Choice(stringResource(R.string.plans), PlanKind.entries.map { stringResource(it.labelResource()) }, kind.ordinal, { kind = PlanKind.entries[it] })
        Button({ navigate("plan/new/${kind.name}") }) { Text(stringResource(R.string.create_plan)) }
        val plans = state.data.plans.filter { it.kind == kind }
        if (plans.isEmpty()) EmptyPanel(stringResource(kind.labelResource()), stringResource(R.string.plans_empty))
        plans.forEach { plan -> PlanCard(plan, state) { navigate("plan_details/${plan.id}") } }
    }
}
@Composable
fun PlanCard(plan: FinancePlan, state: FinanceState, onClick: () -> Unit) {
    val used = if (plan.kind == PlanKind.BUDGET) budgetSpent(plan, state.transactions) else goalSaved(plan, state.data.contributions)
    val limit = BigInteger.valueOf(plan.amountMinor)
    LedgerCard { Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(plan.name, style = MaterialTheme.typography.titleLarge)
        Text(formatMoney(plan.amountMinor, plan.currency, appLocale()), style = MaterialTheme.typography.headlineSmall)
        if (plan.kind != PlanKind.RECURRING) {
            LinearProgressIndicator(progress = { progressFraction(used, limit) }, modifier = Modifier.fillMaxWidth(),
                color = if (plan.kind == PlanKind.BUDGET && used > limit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.progress_amount, formatMoney(used, plan.currency, appLocale()), formatMoney(limit - used, plan.currency, appLocale())))
            if (plan.kind == PlanKind.BUDGET && used * BigInteger.TEN >= limit * BigInteger.valueOf(8)) Text(stringResource(R.string.budget_warning), color = MaterialTheme.colorScheme.error)
        }
        Text(plan.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale())), style = MaterialTheme.typography.bodySmall)
    } }
}
@Composable
fun PlanForm(existing: FinancePlan?, kind: PlanKind, default: LedgerCurrency, busy: Boolean, save: (FinancePlan) -> Unit) {
    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(existing?.let { BigDecimal.valueOf(it.amountMinor,2).toPlainString() }.orEmpty()) }
    var currency by rememberSaveable { mutableStateOf(existing?.currency ?: default.name) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Category.FOOD) }
    var date by rememberSaveable { mutableStateOf((existing?.date ?: LocalDate.now()).toString()) }
    val minor = AddTransaction.parseAmount(amount); val day = runCatching { LocalDate.parse(date) }.getOrNull()
    FinancePage {
        Text(stringResource(kind.labelResource()), style = MaterialTheme.typography.headlineMedium)
        Field(name, { name = it }, R.string.display_name)
        Field(amount, { amount = it }, R.string.target_amount, true)
        CurrencySelector(LedgerCurrency.valueOf(currency), existing == null, onSelect = { currency = it.name })
        if (kind != PlanKind.GOAL) {
            val categories = Category.entries.filter { it.type == TransactionType.EXPENSE }
            Choice(stringResource(R.string.category), categories.map { stringResource(it.labelResource()) }, categories.indexOf(category), { category = categories[it] })
        }
        Field(date, { date = it }, R.string.iso_date)
        Text(stringResource(when(kind) { PlanKind.BUDGET -> R.string.budget_date_hint; PlanKind.GOAL -> R.string.goal_hint; PlanKind.RECURRING -> R.string.recurring_hint }))
        Button({ save(FinancePlan(id,kind,name,currency,minor!!,category,day!!)) }, enabled = !busy && name.isNotBlank() && name.length <= 60 && minor != null && day != null, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_changes)) }
    }
}
@Composable
fun PlanDetails(plan: FinancePlan, state: FinanceState, busy: Boolean, navigate: (String) -> Unit,
    contribute: (Contribution, () -> Unit) -> Unit, delete: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }; var confirm by rememberSaveable { mutableStateOf(false) }
    var contributionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    FinancePage {
        PlanCard(plan,state) {}
        Row { TextButton({ navigate("plan/${plan.id}/${plan.kind.name}") }) { Text(stringResource(R.string.edit)) }
            TextButton({ confirm = true }) { Text(stringResource(R.string.delete)) } }
        when(plan.kind) {
            PlanKind.BUDGET -> {
                Text(stringResource(R.string.transactions), style = MaterialTheme.typography.titleMedium)
                state.transactions.filter { it.type == TransactionType.EXPENSE && it.category == plan.category && it.currency == plan.currency && YearMonth.from(it.date) == YearMonth.from(plan.date) }
                    .forEach { TransactionCard(it, { navigate("transaction/${it.id}") }) }
                Text(stringResource(R.string.monthly_trend), style = MaterialTheme.typography.titleMedium)
                (0L..5L).forEach { offset ->
                    val month = YearMonth.from(plan.date).minusMonths(offset)
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",appLocale())) + " · " + formatMoney(budgetSpent(plan.copy(date = month.atDay(1)),state.transactions),plan.currency,appLocale()))
                }
            }
            PlanKind.GOAL -> {
                Text(stringResource(R.string.goal_hint))
                Field(amount, { amount = it }, R.string.contribution, true)
                Button({ contribute(Contribution(contributionId,plan.id,AddTransaction.parseAmount(amount)!!,LocalDate.now())) { amount = ""; contributionId = UUID.randomUUID().toString() } }, enabled = !busy && AddTransaction.parseAmount(amount) != null) { Text(stringResource(R.string.contribute)) }
                val estimate = goalEstimate(plan,state.data.contributions,LocalDate.now())
                Text(estimate?.let { stringResource(R.string.estimated_completion,it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale()))) } ?: stringResource(R.string.estimate_unavailable))
                state.data.contributions.filter { it.goalId == plan.id }.forEach {
                    Text(it.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale())) + " · " + formatMoney(it.amountMinor,plan.currency,appLocale()))
                }
            }
            PlanKind.RECURRING -> {
                Text(stringResource(R.string.recurring_hint))
                upcomingPayments(plan.date,LocalDate.now()).forEach { due ->
                    Text(due.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(appLocale())) + " · " + formatMoney(plan.amountMinor,plan.currency,appLocale()))
                }
            }
        }
    }
    if (confirm) ConfirmDelete({ confirm = false }) { delete(); confirm = false }
}
@Composable
fun ConfirmDelete(dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.delete)) },
        text = { Text(stringResource(R.string.delete_finance_hint)) },
        confirmButton = { TextButton(confirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(dismiss) { Text(stringResource(R.string.cancel)) } })
}
@Composable
fun ConnectScreen() { FinancePage {
    Text(stringResource(R.string.connect_bank), style = MaterialTheme.typography.headlineLarge)
    EmptyPanel(stringResource(R.string.manually_managed), stringResource(R.string.bank_connection_hint))
} }
