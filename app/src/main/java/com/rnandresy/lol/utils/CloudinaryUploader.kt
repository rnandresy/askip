package com.rnandresy.lol.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// ⚠️ Remplace par tes valeurs Cloudinary
private const val CLOUD_NAME    = "di6bq2h1d"
private const val UPLOAD_PRESET = "postaskip"

object CloudinaryUploader {

    private const val BOUNDARY   = "AskipBoundary7C4DFF"
    private const val LINE_END   = "\r\n"
    private const val TWO_DASHES = "--"

    // ── Méthodes publiques ────────────────────────────────────────────────────

    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            val config = mapOf(
                "di6bq2h1d" to CLOUD_NAME, // Utilise votre constante "di6bq2h1d"
                "secure"     to true
            )
            try {
                MediaManager.init(context, config)
                initialized = true
            } catch (e: Exception) {
                // Évite le crash si déjà initialisé par ailleurs
                initialized = true
            }
        }
    }

    suspend fun uploadImage(
        context: Context, uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): String {
        val (bytes, mime, name) = readUri(context, uri)
        return uploadBytes(bytes, mime, name, resourceEndpoint("image"), onProgress)
    }

    suspend fun uploadVideo(
        context: Context, uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): String {
        val (bytes, mime, name) = readUri(context, uri)
        return uploadBytes(bytes, mime, name, resourceEndpoint("video"), onProgress)
    }

    /** Upload un fichier audio (m4a) depuis un File local (après enregistrement) */
    suspend fun uploadAudio(
        file: File,
        onProgress: ((Int) -> Unit)? = null
    ): String {
        val bytes = file.readBytes()
        return uploadBytes(
            bytes, "audio/m4a", file.name,
            resourceEndpoint("video"),    // Cloudinary gère l'audio via le endpoint video
            onProgress
        )
    }

    /** Upload un fichier quelconque (PDF, doc, etc.) via l'URI Android */
    suspend fun uploadFile(
        context: Context, uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Pair<String, String> {          // URL, nom original
        val (bytes, mime, name) = readUri(context, uri)
        val url = uploadBytes(bytes, mime, name, resourceEndpoint("raw"), onProgress)
        return url to name
    }

    // ── Interne ───────────────────────────────────────────────────────────────

    private fun resourceEndpoint(type: String) =
        "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$type/upload"

    private data class UriData(val bytes: ByteArray, val mime: String, val name: String)

    private fun readUri(context: Context, uri: Uri): UriData {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Impossible de lire le fichier")
        val mime  = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val name  = getFileName(context, uri)
        return UriData(bytes, mime, name)
    }

    private suspend fun uploadBytes(
        bytes: ByteArray,
        mime: String,
        fileName: String,
        apiUrl: String,
        onProgress: ((Int) -> Unit)?
    ): String = withContext(Dispatchers.IO) {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            doInput        = true
            doOutput       = true
            useCaches      = false
            requestMethod  = "POST"
            connectTimeout = 30_000
            readTimeout    = 180_000
            setRequestProperty("Connection", "Keep-Alive")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
        }

        DataOutputStream(conn.outputStream).use { out ->
            // upload_preset
            out.writeBytes("$TWO_DASHES$BOUNDARY$LINE_END")
            out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"$LINE_END$LINE_END")
            out.writeBytes(UPLOAD_PRESET)
            out.writeBytes(LINE_END)
            // file
            out.writeBytes("$TWO_DASHES$BOUNDARY$LINE_END")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"$LINE_END")
            out.writeBytes("Content-Type: $mime$LINE_END$LINE_END")
            // écriture par chunks avec progression
            val chunk = 8192
            var offset = 0
            while (offset < bytes.size) {
                val end = minOf(offset + chunk, bytes.size)
                out.write(bytes, offset, end - offset)
                offset = end
                onProgress?.invoke(offset * 100 / bytes.size)
            }
            out.writeBytes(LINE_END)
            out.writeBytes("$TWO_DASHES$BOUNDARY$TWO_DASHES$LINE_END")
            out.flush()
        }

        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "Erreur $code"
            error("Upload échoué ($code): $err")
        }
        val resp = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        JSONObject(resp).getString("secure_url")
    }

    private fun getFileName(context: Context, uri: Uri): String = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(col)
        }
    }.getOrNull() ?: "file_${System.currentTimeMillis()}"
}