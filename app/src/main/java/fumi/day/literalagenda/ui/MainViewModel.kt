package fumi.day.literalagenda.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalagenda.data.Event
import fumi.day.literalagenda.data.EventRepository
import fumi.day.literalagenda.data.GitSyncRepository
import fumi.day.literalagenda.data.RepeatType
import fumi.day.literalagenda.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository,
    private val gitHubRepository: GitSyncRepository
) : ViewModel() {

    val events: Flow<List<Event>> = eventRepository.getAllEvents().map { filterEvents(it, 0, 3) }
    val allEvents: Flow<List<Event>> = eventRepository.getAllEvents()

    val pastMonths: StateFlow<Int> = settingsRepository.pastMonths
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)
    val futureMonths: StateFlow<Int> = settingsRepository.futureMonths
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    fun setPastMonths(months: Int) { viewModelScope.launch { settingsRepository.setPastMonths(months) } }
    fun setFutureMonths(months: Int) { viewModelScope.launch { settingsRepository.setFutureMonths(months) } }

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

    private fun filterEvents(events: List<Event>, pastMonths: Int, futureMonths: Int): List<Event> {
        val today = LocalDate.now()
        val startDate = if (pastMonths == -1) LocalDate.MIN else today.minusMonths(pastMonths.toLong())
        val endDate = if (futureMonths == -1) today.plusYears(5) else today.plusMonths(futureMonths.toLong())
        val expanded = mutableListOf<Event>()
        events.forEach { event ->
            when (event.repeat) {
                RepeatType.NONE -> {
                    if (!event.date.isBefore(startDate) && !event.date.isAfter(endDate)) {
                        expanded.add(event)
                    }
                }
                RepeatType.WEEKLY -> {
                    var d = event.date
                    while (!d.isAfter(endDate)) {
                        if (!d.isBefore(startDate)) expanded.add(event.copy(date = d))
                        d = d.plusWeeks(1)
                    }
                }
                RepeatType.MONTHLY -> {
                    var d = event.date
                    while (!d.isAfter(endDate)) {
                        if (!d.isBefore(startDate)) expanded.add(event.copy(date = d))
                        d = d.plusMonths(1)
                    }
                }
                RepeatType.YEARLY -> {
                    var d = event.date
                    while (!d.isAfter(endDate)) {
                        if (!d.isBefore(startDate)) expanded.add(event.copy(date = d))
                        d = d.plusYears(1)
                    }
                }
            }
        }
        return expanded.sortedWith(compareBy({ it.date }, { it.time }))
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
