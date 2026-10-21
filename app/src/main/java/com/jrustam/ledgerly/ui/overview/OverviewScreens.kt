package com.jrustam.ledgerly.ui.overview

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.common.*
import com.jrustam.ledgerly.ui.design.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(state: OverviewState, onAdd: () -> Unit, onHistory: () -> Unit,
               onEdit: (String) -> Unit, onRetry: () -> Unit, defaultCurrency: LedgerCurrency) {
    val locale = appLocale()
    val motion = LocalMotionEnabled.current
    if (state.loading || state.failed) { LedgerLoadState(state.loading, onRetry); return }
    val balances = state.summaries.ifEmpty { mapOf(defaultCurrency.name to BalanceSummary()) }
    val currencies = balances.keys.toList()
    var selected by rememberSaveable { mutableStateOf(defaultCurrency.name) }
    val currency = selected.takeIf { it in balances } ?: currencies.first()
    val summary = balances.getValue(currency)
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.your_money), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (currencies.size > 1) item {
            LedgerSelection(currencies, currencies.indexOf(currency), { selected = currencies[it] })
        }
        item {
            CardEntrance {
                Box(Modifier.fillMaxWidth().clip(LedgerShapes.card)
                    .background(Brush.linearGradient(listOf(Color(0xFF123C73), Color(0xFF2464C7), Color(0xFF6261D3))))) {
                    // Decorative light, not a financial chart or data visualization.
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 46.dp, y = (-72).dp).size(200.dp)
                        .background(Color(0xFF65DBEE).copy(alpha = 0.12f), CircleShape))
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.total_balance), color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge)
                            Text(currency, color = Color.White, style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.background(Color.White.copy(alpha = 0.13f), LedgerShapes.small)
                                    .padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                        AnimatedMoney(formatMoney(summary.balanceMinor, currency, locale), MaterialTheme.typography.displaySmall, Color.White)
                        Text(stringResource(R.string.all_time_currency, currency), style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            HeroTotal(stringResource(R.string.total_income), formatMoney(summary.incomeMinor, currency, locale), true, Modifier.weight(1f))
                            HeroTotal(stringResource(R.string.total_expenses), formatMoney(summary.expenseMinor, currency, locale), false, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onHistory) { Text(stringResource(R.string.view_all)) }
            }
        }
        if (state.transactions.isEmpty()) item {
            EmptyPanel(stringResource(R.string.empty_home_title), stringResource(R.string.empty_home_body),
                stringResource(R.string.add_first_transaction), onAdd)
        } else items(state.transactions.take(5), key = { it.id }) { transaction ->
            TransactionCard(transaction, { onEdit(transaction.id) }, modifier = Modifier.animateItem(
                fadeInSpec = tween(if (motion) 180 else 0), fadeOutSpec = tween(if (motion) 120 else 0),
                placementSpec = if (motion) spring(stiffness = 300f) else snap()))
        }
    }
}

@Composable
private fun HeroTotal(title: String, amount: String, income: Boolean, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text((if (income) "↙  " else "↗  ") + title, color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall)
        AnimatedMoney(amount, MaterialTheme.typography.titleMedium, Color.White)
    }
}

@Composable
fun HistoryScreen(state: OverviewState, onFilter: (TransactionFilter) -> Unit, onAdd: () -> Unit,
                  onEdit: (String) -> Unit, onDelete: (Transaction) -> Unit, onRetry: () -> Unit) {
    val locale = appLocale()
    val motion = LocalMotionEnabled.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.history_subtitle), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LedgerSelection(listOf(stringResource(R.string.filter_all), stringResource(R.string.income), stringResource(R.string.expense)),
                state.filter.ordinal, { onFilter(TransactionFilter.entries[it]) })
        }
        if (state.loading || state.failed) LedgerLoadState(state.loading, onRetry)
        else {
            val visible = state.visibleTransactions
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (visible.isEmpty()) item {
                    EmptyPanel(stringResource(R.string.empty_history_title),
                        stringResource(if (state.transactions.isEmpty()) R.string.empty_home_body else R.string.empty_filter_body),
                        stringResource(if (state.transactions.isEmpty()) R.string.add_first_transaction else R.string.clear_filter),
                        { if (state.transactions.isEmpty()) onAdd() else onFilter(TransactionFilter.ALL) })
                }
                visible.groupBy { it.date }.forEach { (date, transactions) ->
                    item(key = "date-$date") {
                        Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionCard(transaction, { onEdit(transaction.id) }, { onDelete(transaction) },
                            Modifier.animateItem(fadeInSpec = tween(if (motion) 180 else 0),
                                fadeOutSpec = tween(if (motion) 120 else 0), placementSpec = if (motion) spring(stiffness = 300f) else snap()))
                    }
                }
            }
        }
    }
}

private fun Category.icon(): ImageVector = when (this) {
    Category.FOOD, Category.SHOPPING -> Icons.Default.ShoppingCart
    Category.TRANSPORT -> Icons.Default.Place
    Category.BILLS -> Icons.Default.DateRange
    Category.HEALTH -> Icons.Default.Favorite
    Category.SALARY -> Icons.Default.AccountBox
    Category.FREELANCE -> Icons.Default.Build
    Category.GIFT -> Icons.Default.Star
    else -> Icons.AutoMirrored.Filled.List
}

@Composable
private fun TransactionCard(transaction: Transaction, onEdit: () -> Unit,
                            onDelete: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val locale = appLocale()
    val income = transaction.type == TransactionType.INCOME
    var expanded by remember { mutableStateOf(false) }
    LedgerCard(modifier.fillMaxWidth()) {
        Column(Modifier.clickable(onClick = onEdit).padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).background(if (income) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    LedgerShapes.control), contentAlignment = Alignment.Center) {
                    Icon(transaction.category.icon(), null, modifier = Modifier.size(19.dp),
                        tint = if (income) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(transaction.category.labelResource()), style = MaterialTheme.typography.titleSmall)
                    Text(transaction.description ?: transaction.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onDelete != null) Box {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.transaction_actions), modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { expanded = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            onClick = { expanded = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 48.dp, top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom) {
                Text(stringResource(if (income) R.string.income else R.string.expense) + " · " + transaction.currency + "\n" + transaction.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                    AnimatedMoney((if (income) "+" else "−") + formatMoney(transaction.amountMinor, transaction.currency, locale),
                        MaterialTheme.typography.titleMedium, if (income) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun EmptyPanel(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) {
    CardEntrance {
        LedgerCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, LedgerShapes.control),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(body, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (action != null) LedgerButton(action, onAction)
            }
        }
    }
}

@Composable
fun LedgerLoadState(loading: Boolean, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        if (loading) {
            if (LocalMotionEnabled.current) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
            else CircularProgressIndicator(progress = { 0.7f }, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        } else EmptyPanel(stringResource(R.string.load_error_title), stringResource(R.string.load_error_body), stringResource(R.string.retry), onRetry)
    }
}

@Composable
fun DeleteTransactionDialog(state: OverviewState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val transaction = state.pendingDelete ?: return
    val locale = appLocale()
    AlertDialog(onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text(stringResource(R.string.delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.delete_body, stringResource(transaction.category.labelResource()),
                    formatMoney(transaction.amountMinor, transaction.currency, locale)))
                if (state.deleteFailed) Text(stringResource(R.string.delete_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.deleting) {
                Text(stringResource(if (state.deleting) R.string.deleting else R.string.delete))
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !state.deleting) { Text(stringResource(R.string.cancel)) } })
}
