package com.example.backend.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID
import com.example.backend.data.MediaFileSignatureValidator

/** File operations shared by the import/download UI and Android instrumentation tests. */
object LocalMediaFileOperations {
    data class CopyResult(val file: File, val mimeType: String, val sizeBytes: Long)

    fun copySelectedMedia(context: Context, source: Uri, outputDirectory: File, title: String): CopyResult {
        val resolver = context.contentResolver
        val mime = resolver.getType(source)?.lowercase()
            ?: throw IllegalArgumentException("The selected document has no media type.")
        require(mime.startsWith("video/") || mime.startsWith("audio/")) {
            "Select an audio or video file. Unsupported media type: $mime"
        }
        require(extensionSupportedMime(mime)) { "The selected media type is not supported: $mime" }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: throw IllegalArgumentException("No file extension is known for $mime.")
        val safeTitle = title.trim().ifBlank { "JoyTube media" }
            .replace(Regex("[^A-Za-z0-9._ -]"), "_").take(100)
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the local media directory.")
        }
        val output = File(outputDirectory, "${safeTitle}_${UUID.randomUUID()}.$extension")
        try {
            val input = resolver.openInputStream(source)
                ?: throw IllegalStateException("The selected file is no longer available.")
            val copied = input.use { sourceStream ->
                output.outputStream().buffered(128 * 1024).use { outputStream -> sourceStream.copyTo(outputStream, 128 * 1024) }
            }
            check(copied > 0L && output.isFile && output.length() == copied) {
                "The copied media file is empty or incomplete."
            }
            require(copied <= MediaFileSignatureValidator.MAX_IMPORT_SIZE_BYTES) {
                "Media file exceeds the ${MediaFileSignatureValidator.MAX_IMPORT_SIZE_BYTES / (1024 * 1024)} MB limit."
            }
            val header = ByteArray(12)
            output.inputStream().use { input ->
                var offset = 0
                while (offset < header.size) {
                    val read = input.read(header, offset, header.size - offset)
                    if (read < 0) break
                    offset += read
                }
            }
            MediaFileSignatureValidator.validateHeaderAndMime(mime, header, copied, "local-device")
                .getOrElse { throw it }
            return CopyResult(output, mime, copied)
        } catch (error: Exception) {
            output.delete()
            throw error
        }
    }

    private fun extensionSupportedMime(mime: String): Boolean = mime in setOf(
        "video/mp4", "audio/mp4", "video/webm", "video/x-matroska", "audio/mpeg", "audio/wav"
    )
}
