package com.jrustam.ledgerly.data.local

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CsvDocumentWriter @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun write(destination: Uri, content: String) = withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openOutputStream(destination, "wt")) {
            "The selected document cannot be opened for writing"
        }.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    }
}
