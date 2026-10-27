package com.jrustam.ledgerly.ui.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.ui.design.*
import com.jrustam.ledgerly.ui.overview.LedgerLoadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.photo(it.toString()) }
    }
    if (state.loading || state.loadFailed) {
        LedgerLoadState(state.loading, viewModel::load)
        return
    }
    val photo by produceState<ImageBitmap?>(null, state.profile.photoPath) {
        value = withContext(Dispatchers.IO) {
            state.profile.photoPath?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
        .padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CardEntrance {
            LedgerGradientCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        if (photo != null) Image(photo!!, stringResource(R.string.profile_photo),
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.Person, stringResource(R.string.profile_photo), Modifier.size(48.dp))
                    }
                    Text(state.profile.displayName.ifBlank { stringResource(R.string.my_profile) },
                        style = MaterialTheme.typography.titleLarge)
                    FilledTonalButton(enabled = !state.busy, onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(R.string.change_photo)) }
                }
            }
        }
        if (state.profile.photoPath != null) TextButton(enabled = !state.busy, onClick = { viewModel.photo(null) },
            modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.remove_photo)) }
        LedgerCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.name, onValueChange = viewModel::editName,
                    modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.display_name)) },
                    enabled = !state.busy, singleLine = true, isError = state.nameError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    supportingText = { Text(stringResource(if (state.nameError) R.string.profile_name_error else R.string.profile_name_hint)) },
                    shape = LedgerShapes.control)
                LedgerButton(stringResource(R.string.save_changes), viewModel::saveName,
                    Modifier.fillMaxWidth(), enabled = !state.busy)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.saveFailed) Text(stringResource(R.string.profile_save_error), color = MaterialTheme.colorScheme.error)
                if (state.saved) Text(stringResource(R.string.profile_saved), color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(stringResource(R.string.profile_privacy), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
