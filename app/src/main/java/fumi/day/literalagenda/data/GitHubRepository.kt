package fumi.day.literalagenda.data

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubFile(
    val name: String,
    val path: String,
    val sha: String
)

@Singleton
class GitHubRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun launchSync() {
        if (_isSyncing.value) return
        appScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val result = sync()
                if (result is SyncResult.Error) {
                    _syncError.value = result.message
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    private val baseUrl = "https://api.github.com"

    private val eventsDir: File
        get() = File(context.filesDir, "events").also { it.mkdirs() }

    private val repeatingDir: File
        get() = File(context.filesDir, "repeating").also { it.mkdirs() }

    private suspend fun makeRequest(
        method: String,
        url: String,
        token: String,
        body: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                }
            }

            val responseCode = connection.responseCode
            val responseBody = try {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            }

            Pair(responseCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun sync(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = settingsRepository.githubToken.first()
                val repo = settingsRepository.githubRepo.first()

                if (token.isBlank() || repo.isBlank()) {
                    return@withContext SyncResult.NotConfigured
                }

                val lastSyncedAt = settingsRepository.lastSyncedAt.first()
                val lastSyncedShas = settingsRepository.lastSyncedShas.first()
                val newShas = mutableMapOf<String, String>()

                syncDirectory(token, repo, "events", eventsDir, lastSyncedAt, lastSyncedShas, newShas)
                syncDirectory(token, repo, "repeating", repeatingDir, lastSyncedAt, lastSyncedShas, newShas)

                settingsRepository.setLastSyncedAt(System.currentTimeMillis())
                settingsRepository.setLastSyncedShas(newShas)

                SyncResult.Success
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun syncDirectory(
        token: String,
        repo: String,
        remotePath: String,
        localDir: File,
        lastSyncedAt: Long,
        lastSyncedShas: Map<String, String>,
        newShas: MutableMap<String, String>
    ) {
        val remoteFiles = listFilesInDir(token, repo, remotePath)
        val remoteFileMap = remoteFiles.associateBy { it.name }
        remoteFiles.forEach { newShas["$remotePath/${it.name}"] = it.sha }

        val localFiles = localDir.listFiles()?.filter { it.extension == "md" } ?: emptyList()
        val localFileMap = localFiles.associateBy { it.name }

        val allFileNames = (localFileMap.keys + remoteFileMap.keys).toSet()

        for (fileName in allFileNames) {
            val localFile = localFileMap[fileName]
            val remoteFile = remoteFileMap[fileName]
            val knownSha = lastSyncedShas["$remotePath/$fileName"]

            when {
                localFile != null && remoteFile == null -> {
                    if (knownSha != null) {
                        // Previously synced, now deleted on remote → delete locally
                        localFile.delete()
                        newShas.remove("$remotePath/$fileName")
                    } else {
                        // New local file → upload
                        val content = localFile.readText(Charsets.UTF_8)
                        val newSha = putFile(token, repo, "$remotePath/$fileName", content, null)
                        if (newSha != null) newShas["$remotePath/$fileName"] = newSha
                    }
                }

                localFile == null && remoteFile != null -> {
                    if (knownSha != null) {
                        // Previously synced, now deleted locally → delete from remote
                        deleteFile(token, repo, "$remotePath/$fileName", remoteFile.sha)
                        newShas.remove("$remotePath/$fileName")
                    } else {
                        // New remote file → download
                        val content = getFileContent(token, repo, remoteFile.path)
                        if (content != null) {
                            File(localDir, fileName).writeText(content, Charsets.UTF_8)
                        }
                    }
                }

                localFile != null && remoteFile != null -> {
                    val remoteChanged = knownSha != null && knownSha != remoteFile.sha
                    val localChanged = knownSha != null && localFile.lastModified() > lastSyncedAt

                    when {
                        knownSha == null -> {
                            // First sync with both sides present: remote wins
                            val remoteContent = getFileContent(token, repo, remoteFile.path)
                            if (remoteContent != null && remoteContent != localFile.readText(Charsets.UTF_8)) {
                                localFile.writeText(remoteContent, Charsets.UTF_8)
                            }
                        }
                        !localChanged && !remoteChanged -> { /* in sync, nothing to do */ }
                        localChanged && !remoteChanged -> {
                            // Only local changed → upload
                            val content = localFile.readText(Charsets.UTF_8)
                            val newSha = putFile(token, repo, "$remotePath/$fileName", content, remoteFile.sha)
                            if (newSha != null) newShas["$remotePath/$fileName"] = newSha
                        }
                        !localChanged && remoteChanged -> {
                            // Only remote changed → download
                            val content = getFileContent(token, repo, remoteFile.path)
                            if (content != null) localFile.writeText(content, Charsets.UTF_8)
                        }
                        else -> {
                            // Both changed → conflict: save local copy, download remote
                            val localContent = localFile.readText(Charsets.UTF_8)
                            val remoteContent = getFileContent(token, repo, remoteFile.path)
                            if (remoteContent != null) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val conflictName = fileName.removeSuffix(".md") + "_conflict_$timestamp.md"
                                File(localDir, conflictName).writeText(localContent, Charsets.UTF_8)
                                localFile.writeText(remoteContent, Charsets.UTF_8)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun listFilesInDir(token: String, repo: String, dir: String): List<GitHubFile> {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$dir"
            val (code, body) = makeRequest("GET", url, token)

            when (code) {
                200 -> {
                    val files = mutableListOf<GitHubFile>()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val name = obj.getString("name")
                        if (name.endsWith(".md")) {
                            files.add(
                                GitHubFile(
                                    name = name,
                                    path = obj.getString("path"),
                                    sha = obj.getString("sha")
                                )
                            )
                        }
                    }
                    files
                }
                404 -> emptyList()
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getFileContent(token: String, repo: String, path: String): String? {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$path"
            val (code, body) = makeRequest("GET", url, token)

            when (code) {
                200 -> {
                    val obj = JSONObject(body)
                    val contentBase64 = obj.getString("content").replace("\n", "")
                    String(Base64.decode(contentBase64, Base64.DEFAULT), Charsets.UTF_8)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Returns the new SHA on success, null on failure
    private suspend fun putFile(
        token: String,
        repo: String,
        path: String,
        content: String,
        sha: String?
    ): String? {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$path"
            val contentBase64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val bodyObj = JSONObject().apply {
                put("message", if (sha == null) "Create $path" else "Update $path")
                put("content", contentBase64)
                if (sha != null) put("sha", sha)
            }

            val (code, body) = makeRequest("PUT", url, token, bodyObj.toString())
            if (code == 200 || code == 201) {
                JSONObject(body).getJSONObject("content").getString("sha")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun deleteFile(token: String, repo: String, path: String, sha: String): Boolean {
        return try {
            val url = "$baseUrl/repos/$repo/contents/$path"
            val bodyObj = JSONObject().apply {
                put("message", "Delete $path")
                put("sha", sha)
            }
            val (code, _) = makeRequest("DELETE", url, token, bodyObj.toString())
            code == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteRemoteFile(filename: String, isRepeating: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = settingsRepository.githubToken.first()
                val repo = settingsRepository.githubRepo.first()

                if (token.isBlank() || repo.isBlank()) return@withContext true

                val dir = if (isRepeating) "repeating" else "events"
                val remoteFiles = listFilesInDir(token, repo, dir)
                val remoteFile = remoteFiles.find { it.name == filename } ?: return@withContext true

                deleteFile(token, repo, "$dir/$filename", remoteFile.sha)
            } catch (e: Exception) {
                false
            }
        }
    }
}

sealed class SyncResult {
    object Success : SyncResult()
    object NotConfigured : SyncResult()
    data class Error(val message: String) : SyncResult()
}
