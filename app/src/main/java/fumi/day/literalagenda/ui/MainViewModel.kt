package fumi.day.literalagenda.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalagenda.data.Event
import fumi.day.literalagenda.data.EventRepository
import fumi.day.literalagenda.data.GitSyncRepository
import fumi.day.literalagenda.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository,
    private val gitHubRepository: GitSyncRepository
) : ViewModel() {

    val events: Flow<List<Event>> = eventRepository.getUpcomingEvents()
    val allEvents: Flow<List<Event>> = eventRepository.getAllEvents()

    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent

    fun selectEvent(event: Event?) {
        _selectedEvent.value = event
    }
    val showMiniCalendar: Flow<Boolean> = settingsRepository.showMiniCalendar
    val controlsOnLeft: Flow<Boolean> = settingsRepository.controlsOnLeft
    val dateFormat: Flow<String> = settingsRepository.dateFormat

    val syncState: StateFlow<SyncState> = combine(
        gitHubRepository.isSyncing,
        gitHubRepository.syncError
    ) { isSyncing, error ->
        when {
            isSyncing -> SyncState.Syncing
            error != null -> SyncState.Error(error)
            else -> SyncState.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncState.Idle)

    private val _lastSyncedAt = MutableStateFlow<String>("")
    val lastSyncedAt: StateFlow<String> = _lastSyncedAt

    init {
        viewModelScope.launch {
            eventRepository.loadEvents()
        }
        viewModelScope.launch {
            var wasSyncing = false
            gitHubRepository.isSyncing.collect { syncing ->
                if (wasSyncing && !syncing) {
                    eventRepository.loadEvents()
                    if (gitHubRepository.syncError.value == null) {
                        _lastSyncedAt.value = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    }
                }
                wasSyncing = syncing
            }
        }
    }

    fun syncAndLoad() {
        viewModelScope.launch { eventRepository.loadEvents() }
        gitHubRepository.launchSync()
    }

    fun clearSyncState() {
        gitHubRepository.clearSyncError()
    }

    fun toggleMiniCalendar() {
        viewModelScope.launch {
            val current = settingsRepository.showMiniCalendar.first()
            settingsRepository.setShowMiniCalendar(!current)
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
}
