package fumi.day.literalagenda.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitSyncRepository @Inject constructor(
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

    fun clearLocalData() {
        eventsDir.listFiles()?.forEach { it.delete() }
        repeatingDir.listFiles()?.forEach { it.delete() }
    }

    private val eventsDir: File
        get() = File(context.filesDir, "events").also { it.mkdirs() }

    private val repeatingDir: File
        get() = File(context.filesDir, "repeating").also { it.mkdirs() }

    private suspend fun resolveApi(): Triple<GitForgeApi, String, String>? {
        val token = settingsRepository.gitToken.first()
        val repo = settingsRepository.gitRepo.first()
        val forge = settingsRepository.gitForge.first()
        val host = settingsRepository.gitHost.first()

        if (token.isBlank() || repo.isBlank()) return null
        if (forge == GitForge.GITEA && host.isBlank()) return null

        val api: GitForgeApi = when (forge) {
            GitForge.GITHUB -> GitHubApi()
            GitForge.GITEA -> GiteaApi(host)
        }
        return Triple(api, token, repo)
    }

    suspend fun sync(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val (api, token, repo) = resolveApi() ?: return@withContext SyncResult.NotConfigured

                val lastSyncedAt = settingsRepository.lastSyncedAt.first()
                val lastSyncedShas = settingsRepository.lastSyncedShas.first()
                val newShas = mutableMapOf<String, String>()

                syncDirectory(api, token, repo, "events", eventsDir, lastSyncedAt, lastSyncedShas, newShas)
                syncDirectory(api, token, repo, "repeating", repeatingDir, lastSyncedAt, lastSyncedShas, newShas)

                settingsRepository.setLastSyncedAt(System.currentTimeMillis())
                settingsRepository.setLastSyncedShas(newShas)

                SyncResult.Success
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun syncDirectory(
        api: GitForgeApi,
        token: String,
        repo: String,
        remotePath: String,
        localDir: File,
        lastSyncedAt: Long,
        lastSyncedShas: Map<String, String>,
        newShas: MutableMap<String, String>
    ) {
        val remoteFiles = api.listFiles(token, repo, remotePath)
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
                        // Previously synced, now gone from remote → delete locally
                        localFile.delete()
                        newShas.remove("$remotePath/$fileName")
                    } else {
                        // New local file → upload
                        val content = localFile.readText(Charsets.UTF_8)
                        val newSha = api.putFile(token, repo, "$remotePath/$fileName", content, null)
                        if (newSha != null) newShas["$remotePath/$fileName"] = newSha
                    }
                }

                localFile == null && remoteFile != null -> {
                    if (knownSha != null) {
                        // Previously synced, now deleted locally → delete from remote
                        api.deleteFile(token, repo, "$remotePath/$fileName", remoteFile.sha)
                        newShas.remove("$remotePath/$fileName")
                    } else {
                        // New remote file → download
                        val content = api.getFileContent(token, repo, remoteFile.path)
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
                            val remoteContent = api.getFileContent(token, repo, remoteFile.path)
                            if (remoteContent != null && remoteContent != localFile.readText(Charsets.UTF_8)) {
                                localFile.writeText(remoteContent, Charsets.UTF_8)
                            }
                        }
                        !localChanged && !remoteChanged -> { /* in sync, nothing to do */ }
                        localChanged && !remoteChanged -> {
                            // Only local changed → upload
                            val content = localFile.readText(Charsets.UTF_8)
                            val newSha = api.putFile(token, repo, "$remotePath/$fileName", content, remoteFile.sha)
                            if (newSha != null) newShas["$remotePath/$fileName"] = newSha
                        }
                        !localChanged && remoteChanged -> {
                            // Only remote changed → download
                            val content = api.getFileContent(token, repo, remoteFile.path)
                            if (content != null) localFile.writeText(content, Charsets.UTF_8)
                        }
                        else -> {
                            // Both changed → conflict: save local copy, download remote
                            val localContent = localFile.readText(Charsets.UTF_8)
                            val remoteContent = api.getFileContent(token, repo, remoteFile.path)
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

    suspend fun moveToRemoteTrash(filename: String, isRepeating: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val (api, token, repo) = resolveApi() ?: return@withContext true

                val dir = if (isRepeating) "repeating" else "events"
                val remoteFiles = api.listFiles(token, repo, dir)
                val remoteFile = remoteFiles.find { it.name == filename } ?: return@withContext true

                val content = api.getFileContent(token, repo, remoteFile.path) ?: return@withContext false
                api.putFile(token, repo, "trash/$filename", content, null)
                api.deleteFile(token, repo, "$dir/$filename", remoteFile.sha)
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
