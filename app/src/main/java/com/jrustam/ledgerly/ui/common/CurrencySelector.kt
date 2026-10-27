package com.jrustam.ledgerly.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.ui.design.LedgerShapes

@Composable
fun CurrencySelector(
    currency: LedgerCurrency, enabled: Boolean = true, compact: Boolean = false,
    onSelect: (LedgerCurrency) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true }, enabled = enabled, shape = LedgerShapes.control,
            modifier = (if (compact) Modifier else Modifier.fillMaxWidth()).heightIn(min = 48.dp)
        ) {
            Text(
                if (compact) currency.name else stringResource(
                    R.string.currency_value,
                    currency.name,
                    stringResource(currency.labelResource())
                ),
                modifier = if (compact) Modifier else Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LedgerCurrency.entries.forEach { value ->
                DropdownMenuItem(
                    text = {
                    Text(
                        stringResource(
                            R.string.currency_value,
                            value.name,
                            stringResource(value.labelResource())
                        )
                    )
                }, trailingIcon = { if (value == currency) Icon(Icons.Default.Check, null) },
                    onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}
