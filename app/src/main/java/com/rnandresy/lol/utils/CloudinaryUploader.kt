package com.rnandresy.lol.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Upload vers Cloudinary sans SDK (économise ~2 Mo d'APK).
 * Les images sont compressées automatiquement avant envoi.
 *
 * ⚠️ Renseigner CLOUD_NAME et UPLOAD_PRESET.
 */
object CloudinaryUploader {

    private const val CLOUD_NAME    = "di6bq2h1d"
    private const val UPLOAD_PRESET = "postaskip"

    private const val BOUNDARY = "AskipBoundary7F3A9C"
    private const val CRLF     = "\r\n"
    private const val DASHES   = "--"

    data class UploadResult(
        val url: String,
        val bytesUploaded: Int,
        val savedKb: Int = 0
    )

    // ── Image (compressée) ────────────────────────────────────────────────────
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult {
        val compressed = ImageCompressor.compress(context, uri)
        val url = post(
            bytes      = compressed.bytes,
            mime       = "image/jpeg",
            fileName   = "img_${System.currentTimeMillis()}.jpg",
            endpoint   = endpoint("image"),
            onProgress = onProgress
        )
        return UploadResult(
            url           = url,
            bytesUploaded = compressed.bytes.size,
            savedKb       = compressed.originalSizeKb - compressed.compressedSizeKb
        )
    }

    // ── Vidéo ─────────────────────────────────────────────────────────────────
    suspend fun uploadVideo(
        context: Context,
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult {
        val (bytes, mime, name) = read(context, uri)
        require(bytes.size <= MAX_UPLOAD_SIZE_MB * 1024 * 1024) {
            "Vidéo trop lourde (max $MAX_UPLOAD_SIZE_MB Mo)"
        }
        val url = post(bytes, mime, name, endpoint("video"), onProgress)
        return UploadResult(url, bytes.size)
    }

    // ── Audio (fichier local) ─────────────────────────────────────────────────
    suspend fun uploadAudio(
        file: File,
        onProgress: ((Int) -> Unit)? = null
    ): UploadResult {
        val bytes = file.readBytes()
        val url   = post(bytes, "audio/mp4", file.name, endpoint("video"), onProgress)
        return UploadResult(url, bytes.size)
    }

    // ── Fichier quelconque ────────────────────────────────────────────────────
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Pair<UploadResult, String> {
        val (bytes, mime, name) = read(context, uri)
        require(bytes.size <= MAX_UPLOAD_SIZE_MB * 1024 * 1024) {
            "Fichier trop lourd (max $MAX_UPLOAD_SIZE_MB Mo)"
        }
        val url = post(bytes, mime, name, endpoint("raw"), onProgress)
        return UploadResult(url, bytes.size) to name
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    private fun endpoint(type: String) =
        "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$type/upload"

    private data class FileData(val bytes: ByteArray, val mime: String, val name: String) {
        override fun equals(other: Any?) =
            other is FileData && bytes.contentEquals(other.bytes)
        override fun hashCode() = bytes.contentHashCode()
    }

    private fun read(context: Context, uri: Uri): FileData {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Impossible de lire le fichier")
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        return FileData(bytes, mime, fileName(context, uri))
    }

    private suspend fun post(
        bytes: ByteArray,
        mime: String,
        fileName: String,
        endpoint: String,
        onProgress: ((Int) -> Unit)?
    ): String = withContext(Dispatchers.IO) {

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            doInput         = true
            doOutput        = true
            useCaches       = false
            requestMethod   = "POST"
            connectTimeout  = 20_000
            readTimeout     = 180_000
            setFixedLengthStreamingMode(estimateLength(bytes.size, fileName, mime))
            setRequestProperty("Connection", "Keep-Alive")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
        }

        runCatching {
            DataOutputStream(conn.outputStream.buffered(16 * 1024)).use { out ->
                // Champ upload_preset
                out.writeBytes("$DASHES$BOUNDARY$CRLF")
                out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"$CRLF")
                out.writeBytes(CRLF)
                out.writeBytes(UPLOAD_PRESET)
                out.writeBytes(CRLF)

                // Champ file
                out.writeBytes("$DASHES$BOUNDARY$CRLF")
                out.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$CRLF"
                )
                out.writeBytes("Content-Type: $mime$CRLF")
                out.writeBytes(CRLF)

                // Écriture par blocs avec progression
                val chunk = 16 * 1024
                var sent  = 0
                var lastReported = -1
                while (sent < bytes.size) {
                    val end = minOf(sent + chunk, bytes.size)
                    out.write(bytes, sent, end - sent)
                    sent = end
                    val pct = sent * 100 / bytes.size
                    if (pct != lastReported) {
                        onProgress?.invoke(pct)
                        lastReported = pct
                    }
                }

                out.writeBytes(CRLF)
                out.writeBytes("$DASHES$BOUNDARY$DASHES$CRLF")
                out.flush()
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                val err = conn.errorStream
                    ?.let { BufferedReader(InputStreamReader(it)).readText() }
                    ?: "Code ${conn.responseCode}"
                error("Upload échoué : $err")
            }

            val body = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            JSONObject(body).getString("secure_url")

        }.also { conn.disconnect() }.getOrThrow()
    }

    private fun estimateLength(fileSize: Int, fileName: String, mime: String): Long {
        val preset = "$DASHES$BOUNDARY$CRLF" +
                "Content-Disposition: form-data; name=\"upload_preset\"$CRLF$CRLF" +
                "$UPLOAD_PRESET$CRLF"
        val header = "$DASHES$BOUNDARY$CRLF" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$CRLF" +
                "Content-Type: $mime$CRLF$CRLF"
        val footer = "$CRLF$DASHES$BOUNDARY$DASHES$CRLF"
        return (preset.length + header.length + fileSize + footer.length).toLong()
    }

    private fun fileName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }.getOrNull() ?: "file_${System.currentTimeMillis()}"
}