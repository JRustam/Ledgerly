package com.jrustam.ledgerly.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrustam.ledgerly.R

@Composable
fun ExportAction(enabled: Boolean, viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { destination ->
            destination?.let(viewModel::export)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            enabled = enabled && state != ExportState.Writing,
            onClick = { launcher.launch("ledgerly.csv") }
        ) {
            Text(stringResource(R.string.export_csv))
        }

        when (state) {
            ExportState.Writing -> LinearProgressIndicator()
            ExportState.Success -> Text(
                stringResource(R.string.export_success),
                style = MaterialTheme.typography.bodySmall
            )

            ExportState.Failed -> Text(
                stringResource(R.string.save_error),
                color = MaterialTheme.colorScheme.error
            )

            ExportState.Idle -> Unit
        }
    }
}
