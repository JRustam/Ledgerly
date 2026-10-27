package com.jrustam.ledgerly.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.model.AppLanguage
import com.jrustam.ledgerly.ui.common.labelResource
import com.jrustam.ledgerly.ui.design.LedgerSelectionSurface
import com.jrustam.ledgerly.ui.design.LedgerShapes

// Flags are visual locale cues only; language selection never sets a country or currency.
private val AppLanguage.flag: String
    get() = when (this) {
        AppLanguage.SYSTEM -> "🌐"
        AppLanguage.ENGLISH -> "🇬🇧"
        AppLanguage.AZERBAIJANI -> "🇦🇿"
        AppLanguage.RUSSIAN -> "🇷🇺"
        AppLanguage.TURKISH -> "🇹🇷"
        AppLanguage.UKRAINIAN -> "🇺🇦"
        AppLanguage.SERBIAN_LATIN, AppLanguage.SERBIAN_CYRILLIC -> "🇷🇸"
        AppLanguage.FRENCH -> "🇫🇷"
        AppLanguage.GERMAN -> "🇩🇪"
        AppLanguage.ARABIC -> "🇸🇦"
    }

@Composable
private fun LanguageFlag(language: AppLanguage) {
    Text(language.flag, modifier = Modifier.clearAndSetSemantics { }, fontSize = 22.sp)
}

@Composable
fun LanguageSelector(language: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, shape = LedgerShapes.control,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        LanguageFlag(language)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(language.labelResource()), Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content))
        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp))
    }
    if (expanded) AlertDialog(
        onDismissRequest = { expanded = false },
        title = { Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(AppLanguage.entries, key = { it.tag }) { item ->
                    val selected = language == item
                    LedgerSelectionSurface(selected) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected, role = Role.RadioButton, onClick = {
                                expanded = false
                                onSelect(item)
                            }).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LanguageFlag(item)
                            Text(stringResource(item.labelResource()), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content))
                            if (selected) Icon(Icons.Default.CheckCircle, null, Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.cancel)) } }
    )
}
