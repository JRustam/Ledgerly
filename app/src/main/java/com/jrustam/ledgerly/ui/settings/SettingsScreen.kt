package com.jrustam.ledgerly.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.common.*
import com.jrustam.ledgerly.ui.design.*
import com.jrustam.ledgerly.ui.overview.LedgerLoadState

@Composable
fun SettingsScreen(state: SettingsState, onLanguage: (AppLanguage) -> Unit,
                   onCurrency: (LedgerCurrency) -> Unit, onRetry: () -> Unit) {
    var chooseLanguage by rememberSaveable { mutableStateOf(false) }
    if (state.loading || state.failed) { LedgerLoadState(state.loading, onRetry); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_heading), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.settings_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LedgerCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { chooseLanguage = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(state.language.labelResource()))
                }
                Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LedgerCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.default_currency), style = MaterialTheme.typography.titleMedium)
                CurrencySelector(state.currency, !state.saving, onSelect = onCurrency)
                Text(stringResource(R.string.currency_default_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.saveFailed) Text(stringResource(R.string.settings_save_error), color = MaterialTheme.colorScheme.error)
            }
        }
        Text(stringResource(R.string.settings_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
    }
    if (chooseLanguage) AlertDialog(onDismissRequest = { chooseLanguage = false },
        title = { Text(stringResource(R.string.app_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppLanguage.entries.forEach { language ->
                    Row(Modifier.fillMaxWidth().selectable(selected = state.language == language,
                        role = Role.RadioButton, onClick = { chooseLanguage = false; onLanguage(language) })
                        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.language == language, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(language.labelResource()))
                    }
                }
            }
        }, confirmButton = {
            TextButton(onClick = { chooseLanguage = false }) { Text(stringResource(R.string.cancel)) }
        })
}
