package com.jrustam.ledgerly.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.model.AppLanguage
import com.jrustam.ledgerly.domain.model.Appearance
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.ui.common.CurrencySelector
import com.jrustam.ledgerly.ui.design.*
import com.jrustam.ledgerly.ui.overview.LedgerLoadState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsState, onLanguage: (AppLanguage) -> Unit,
    onCurrency: (LedgerCurrency) -> Unit, onRetry: () -> Unit,
    appearance: Appearance = Appearance.SYSTEM,
    onAppearance: (Appearance) -> Unit = {},
    productError: Boolean = false,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    if (state.loading || state.failed) {
        LedgerLoadState(state.loading, onRetry)
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CardEntrance {
            LedgerGradientCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.settings_heading),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
        SettingsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🌐", modifier = Modifier.clearAndSetSemantics { })
                Text(
                    stringResource(R.string.app_language),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            LanguageSelector(state.language, onLanguage)
            SettingsHint(stringResource(R.string.language_hint))
        }
        SettingsCard {
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Appearance.entries.forEach { option ->
                    FilterChip(
                        selected = option == appearance, onClick = { onAppearance(option) },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        Appearance.SYSTEM -> R.string.appearance_system
                                        Appearance.LIGHT -> R.string.light
                                        Appearance.DARK -> R.string.dark
                                    }
                                )
                            )
                        }, shape = LedgerShapes.control
                    )
                }
            }
            if (productError) Text(
                stringResource(R.string.settings_save_error),
                color = MaterialTheme.colorScheme.error
            )
        }
        SettingsCard {
            Text(
                stringResource(R.string.default_currency),
                style = MaterialTheme.typography.titleSmall
            )
            CurrencySelector(state.currency, !state.saving, onSelect = onCurrency)
            SettingsHint(stringResource(R.string.currency_default_hint))
            if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.saveFailed) Text(
                stringResource(R.string.settings_save_error),
                color = MaterialTheme.colorScheme.error
            )
        }
        SettingsCard(content = actions)
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                null,
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingsHint(stringResource(R.string.settings_privacy), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    LedgerCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
