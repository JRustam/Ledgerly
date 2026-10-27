package com.jrustam.ledgerly.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jrustam.ledgerly.domain.model.UserProfile
import com.jrustam.ledgerly.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DataStoreProfileRepository @Inject constructor(
    private val store: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : ProfileRepository {
    private val nameKey = stringPreferencesKey("profile_name")
    private val photoKey = stringPreferencesKey("profile_photo")
    private val directory get() = File(context.filesDir, "profile")
    private val photoLock = Mutex()
    private fun photoFile(name: String): File? =
        name.takeIf { it.matches(Regex("[a-f0-9-]+\\.jpg")) }?.let { File(directory, it) }

    override val profile = store.data.map { prefs ->
        UserProfile(prefs[nameKey].orEmpty(), prefs[photoKey]?.let(::photoFile)?.absolutePath)
    }

    override suspend fun saveName(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 60)
        store.edit { it[nameKey] = trimmed }
    }

    override suspend fun setPhoto(source: String?) = withContext(Dispatchers.IO) {
        photoLock.withLock {
            val old = store.data.first()[photoKey]?.let(::photoFile)
            if (source == null) {
                store.edit { it.remove(photoKey) }
                old?.delete()
            } else {
                check(directory.isDirectory || directory.mkdirs())
                val input = File.createTempFile("selected-", ".tmp", context.cacheDir)
                val target = File(directory, "${UUID.randomUUID()}.jpg")
                var committed = false
                try {
                    requireNotNull(context.contentResolver.openInputStream(Uri.parse(source))).use { stream ->
                        input.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var total = 0L
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= 20L * 1024 * 1024)
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    val bitmap = decodePhoto(input)
                    try {
                        target.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                    } finally { bitmap.recycle() }
                    withContext(NonCancellable) {
                        store.edit { it[photoKey] = target.name }
                        committed = true
                        old?.delete()
                    }
                } finally {
                    input.delete()
                    if (!committed) target.delete()
                }
            }
            Unit
        }
    }

    private fun decodePhoto(file: File): Bitmap {
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val scale = minOf(1.0, 1024.0 / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        require(options.outWidth > 0 && options.outHeight > 0)
        options.inJustDecodeBounds = false
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path, options))
        val orientation = runCatching {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return bitmap
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } finally { bitmap.recycle() }
    }
}
