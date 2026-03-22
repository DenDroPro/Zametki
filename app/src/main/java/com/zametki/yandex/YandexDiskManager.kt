package com.zametki.yandex

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object YandexDiskManager {

    // TODO: Replace with your Yandex OAuth Client ID
    const val CLIENT_ID = "112c2818143043b59937d5b1f5cc107f"

    private const val PREFS_NAME = "yandex_disk_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val BACKUP_FOLDER = "/Заметки"

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_TOKEN).apply()
    }

    fun isLoggedIn(context: Context): Boolean = !getToken(context).isNullOrBlank()

    fun buildAuthUrl(): String {
        return "https://oauth.yandex.ru/authorize?response_type=token&client_id=$CLIENT_ID"
    }

    fun openAuthInBrowser(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthUrl()))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Ensure a folder exists on Yandex Disk
     */
    suspend fun ensureFolder(token: String, path: String = BACKUP_FOLDER): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://cloud-api.yandex.net/v1/disk/resources?path=${Uri.encode(path)}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Authorization", "OAuth $token")
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299 || code == 409 // 409 = already exists
        } catch (e: Exception) {
            false
        }
    }

    /**
     * List folders inside a given path on Yandex Disk
     */
    suspend fun listFolders(token: String, path: String = "/"): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://cloud-api.yandex.net/v1/disk/resources?path=${Uri.encode(path)}&limit=100&fields=_embedded.items.name,_embedded.items.type,_embedded.items.path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext Result.success(emptyList())
            }
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val obj = JSONObject(json)
            val embedded = obj.optJSONObject("_embedded")
            val items = embedded?.optJSONArray("items") ?: JSONArray()
            val folders = mutableListOf<String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.optString("type") == "dir") {
                    folders.add(item.getString("name"))
                }
            }
            Result.success(folders.sorted())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new folder on Yandex Disk
     */
    suspend fun createFolder(token: String, path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://cloud-api.yandex.net/v1/disk/resources?path=${Uri.encode(path)}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Authorization", "OAuth $token")
            }
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299 || code == 409) Result.success(true)
            else Result.failure(Exception("Ошибка создания папки: код $code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a JSON backup of all notes to Yandex Disk
     */
    suspend fun uploadBackup(token: String, notesJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            ensureFolder(token)

            // Step 1: Get upload URL
            val path = "$BACKUP_FOLDER/backup.json"
            val getUrlUrl = URL("https://cloud-api.yandex.net/v1/disk/resources/upload?path=${Uri.encode(path)}&overwrite=true")
            val getUrlConn = (getUrlUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
            }

            if (getUrlConn.responseCode != 200) {
                val err = getUrlConn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                getUrlConn.disconnect()
                return@withContext Result.failure(Exception("Failed to get upload URL: $err"))
            }

            val respJson = getUrlConn.inputStream.bufferedReader().readText()
            getUrlConn.disconnect()
            val uploadHref = JSONObject(respJson).getString("href")

            // Step 2: Upload file
            val uploadUrl = URL(uploadHref)
            val uploadConn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            OutputStreamWriter(uploadConn.outputStream, Charsets.UTF_8).use { it.write(notesJson) }

            val uploadCode = uploadConn.responseCode
            uploadConn.disconnect()

            if (uploadCode in 200..299) {
                Result.success("Бэкап загружен на Яндекс Диск")
            } else {
                Result.failure(Exception("Upload failed with code $uploadCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a single note as .txt file to Yandex Disk
     */
    suspend fun uploadTextFile(token: String, fileName: String, content: String, folderPath: String = BACKUP_FOLDER): Result<String> = withContext(Dispatchers.IO) {
        try {
            ensureFolder(token, folderPath)

            val safeName = fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "Без названия" }
            val path = "$folderPath/$safeName.txt"
            val getUrlUrl = URL("https://cloud-api.yandex.net/v1/disk/resources/upload?path=${Uri.encode(path)}&overwrite=true")
            val getUrlConn = (getUrlUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
            }

            if (getUrlConn.responseCode != 200) {
                val err = getUrlConn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                getUrlConn.disconnect()
                return@withContext Result.failure(Exception("Failed to get upload URL: $err"))
            }

            val respJson = getUrlConn.inputStream.bufferedReader().readText()
            getUrlConn.disconnect()
            val uploadHref = JSONObject(respJson).getString("href")

            val uploadUrl = URL(uploadHref)
            val uploadConn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                doOutput = true
            }
            OutputStreamWriter(uploadConn.outputStream, Charsets.UTF_8).use { it.write(content) }

            val uploadCode = uploadConn.responseCode
            uploadConn.disconnect()

            if (uploadCode in 200..299) {
                Result.success("Файл \"$safeName.txt\" сохранён на Яндекс Диск")
            } else {
                Result.failure(Exception("Upload failed with code $uploadCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download backup from Yandex Disk
     */
    suspend fun downloadBackup(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val path = "$BACKUP_FOLDER/backup.json"
            val getUrlUrl = URL("https://cloud-api.yandex.net/v1/disk/resources/download?path=${Uri.encode(path)}")
            val getUrlConn = (getUrlUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
            }

            if (getUrlConn.responseCode != 200) {
                val err = getUrlConn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                getUrlConn.disconnect()
                return@withContext Result.failure(Exception("Failed to get download URL: $err"))
            }

            val respJson = getUrlConn.inputStream.bufferedReader().readText()
            getUrlConn.disconnect()
            val downloadHref = JSONObject(respJson).getString("href")

            // Download file
            val downloadUrl = URL(downloadHref)
            val downloadConn = (downloadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
            }

            if (downloadConn.responseCode != 200) {
                downloadConn.disconnect()
                return@withContext Result.failure(Exception("Download failed"))
            }

            val content = downloadConn.inputStream.bufferedReader().readText()
            downloadConn.disconnect()
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
