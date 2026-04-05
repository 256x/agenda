package fumi.day.literalagenda.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalagenda.data.EventRepository
import fumi.day.literalagenda.data.GitHubRepository
import fumi.day.literalagenda.data.SettingsRepository
import fumi.day.literalagenda.data.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gitHubRepository: GitHubRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    val controlsOnLeft: Flow<Boolean> = settingsRepository.controlsOnLeft
    val githubToken: Flow<String> = settingsRepository.githubToken
    val githubRepo: Flow<String> = settingsRepository.githubRepo
    val bgColor: Flow<String> = settingsRepository.bgColor
    val textColor: Flow<String> = settingsRepository.textColor
    val accentColor: Flow<String> = settingsRepository.accentColor
    val fontChoice: Flow<String> = settingsRepository.fontChoice
    val fontSize: Flow<Float> = settingsRepository.fontSize
    val dateFormat: Flow<String> = settingsRepository.dateFormat

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _importResult = MutableStateFlow<Int?>(null)
    val importResult: StateFlow<Int?> = _importResult

    suspend fun setControlsOnLeft(left: Boolean) = settingsRepository.setControlsOnLeft(left)
    suspend fun setGitHubToken(token: String) = settingsRepository.setGitHubToken(token)
    suspend fun setGitHubRepo(repo: String) = settingsRepository.setGitHubRepo(repo)
    suspend fun setBgColor(color: String) = settingsRepository.setBgColor(color)
    suspend fun setTextColor(color: String) = settingsRepository.setTextColor(color)
    suspend fun setAccentColor(color: String) = settingsRepository.setAccentColor(color)
    suspend fun setFontChoice(font: String) = settingsRepository.setFontChoice(font)
    suspend fun setFontSize(size: Float) = settingsRepository.setFontSize(size)
    suspend fun setDateFormat(format: String) = settingsRepository.setDateFormat(format)

    suspend fun syncNow(): Boolean {
        if (_isSyncing.value) return false
        _isSyncing.value = true
        _syncError.value = null
        return try {
            val result = gitHubRepository.sync()
            when (result) {
                is SyncResult.Success -> {
                    eventRepository.loadEvents()
                    true
                }
                is SyncResult.NotConfigured -> {
                    _syncError.value = "GitHub not configured"
                    false
                }
                is SyncResult.Error -> {
                    _syncError.value = result.message
                    false
                }
            }
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun connectGitHub(token: String, repo: String): Boolean {
        setGitHubToken(token)
        setGitHubRepo(repo)
        return syncNow()
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun importICal(inputStream: InputStream) {
        viewModelScope.launch {
            _isImporting.value = true
            _importResult.value = null
            try {
                val count = eventRepository.importFromICal(inputStream)
                if (count > 0) {
                    gitHubRepository.sync()
                }
                _importResult.value = count
            } finally {
                _isImporting.value = false
            }
        }
    }
}
