package com.jrustam.ledgerly.ui.addtransaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.common.*
import com.jrustam.ledgerly.ui.design.*
import com.jrustam.ledgerly.ui.theme.LedgerlyTheme
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    state: AddTransactionState,
    onAmount: (String) -> Unit,
    onType: (TransactionType) -> Unit,
    onCategory: (Category) -> Unit,
    onDate: (LocalDate) -> Unit,
    onDescription: (String) -> Unit,
    onSave: () -> Unit,
    onCurrency: (LedgerCurrency) -> Unit = {},
    showHeading: Boolean = true
) {
    val locale = appLocale()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = Color.Transparent, modifier = Modifier.imePadding(), bottomBar = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)) {
                LedgerButton(
                    stringResource(if (state.isSaving) R.string.saving else if (state.isEditing) R.string.save_changes else R.string.save_transaction),
                    onSave, Modifier.fillMaxWidth(), enabled = !state.isSaving
                )
            }
        }
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showHeading) Text(
                stringResource(if (state.isEditing) R.string.edit_transaction else R.string.add_transaction),
                style = MaterialTheme.typography.headlineMedium
            )
            LedgerCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.amount_heading),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CurrencySelector(
                            state.currency,
                            !state.isSaving,
                            compact = true,
                            onSelect = onCurrency
                        )
                    }
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = onAmount,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        label = {
                            Text(
                                stringResource(
                                    R.string.amount_label,
                                    state.currency.name
                                )
                            )
                        },
                        placeholder = {
                            Text(
                                java.text.NumberFormat.getNumberInstance(locale)
                                    .apply { minimumFractionDigits = 2 }.format(0)
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displaySmall,
                        shape = LedgerShapes.control,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        visualTransformation = VisualTransformation { value ->
                            TransformedText(
                                AnnotatedString(
                                    value.text.replace(
                                        '.',
                                        DecimalFormatSymbols.getInstance(locale).decimalSeparator
                                    )
                                ), OffsetMapping.Identity
                            )
                        },
                        isError = state.errors.amount,
                        supportingText = if (state.errors.amount) ({ Text(stringResource(R.string.amount_error)) }) else null
                    )
                }
            }
            if (state.isEditing) Text(
                stringResource(R.string.currency_edit_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LedgerSelection(
                listOf(
                    stringResource(R.string.expense),
                    stringResource(R.string.income)
                ),
                state.type.ordinal,
                { onType(TransactionType.entries[it]) },
                enabled = !state.isSaving
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.category),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Category.entries.filter { it.type == state.type }.forEach { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { onCategory(category) },
                            enabled = !state.isSaving,
                            shape = LedgerShapes.small,
                            label = { Text(stringResource(category.labelResource())) })
                    }
                }
                if (state.errors.category) Text(
                    stringResource(R.string.category_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            LedgerCard {
                TextButton(
                    onClick = { showDatePicker = true },
                    enabled = !state.isSaving,
                    shape = LedgerShapes.card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Default.DateRange, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.date),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.date.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
                        )
                    )
                }
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescription,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                shape = LedgerShapes.control,
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.description_hint)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                isError = state.errors.description,
                supportingText = {
                    Text(
                        if (state.errors.description) stringResource(R.string.description_error)
                        else stringResource(R.string.description_count, state.description.length)
                    )
                })
            if (state.saved) Text(
                stringResource(R.string.saved),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.tertiary
            )
            if (state.saveFailed) Text(
                stringResource(R.string.save_error),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error
            )
            Text(
                stringResource(R.string.offline_note),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (showDatePicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                    picker.selectedDateMillis?.let {
                        onDate(
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) { Text(stringResource(R.string.cancel)) }
            }) {
            DatePicker(picker)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddTransactionPreview() {
    LedgerlyTheme { AddTransactionScreen(AddTransactionState(), {}, {}, {}, {}, {}, {}) }
}
