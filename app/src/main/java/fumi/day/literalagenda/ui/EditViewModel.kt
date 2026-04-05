package fumi.day.literalagenda.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalagenda.data.Event
import fumi.day.literalagenda.data.EventRepository
import fumi.day.literalagenda.data.GitHubRepository
import fumi.day.literalagenda.data.RepeatType
import fumi.day.literalagenda.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val gitHubRepository: GitHubRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val controlsOnLeft: Flow<Boolean> = settingsRepository.controlsOnLeft

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    suspend fun saveEvent(
        existingFilename: String?,
        date: LocalDate,
        time: LocalTime?,
        title: String,
        note: String,
        repeat: RepeatType
    ): Boolean {
        if (_isSaving.value) return false
        _isSaving.value = true

        return try {
            val success = if (existingFilename != null) {
                // Edit: Overwrite using saveEvent
                val event = Event(
                    filename = existingFilename,
                    date = date,
                    time = time,
                    title = title,
                    note = note,
                    repeat = repeat
                )
                eventRepository.saveEvent(event)
            } else {
                // Create new
                eventRepository.createEvent(date, time, title, note, repeat)
            }

            if (success) {
                eventRepository.loadEvents()
                gitHubRepository.launchSync()
            }
            success
        } catch (e: Exception) {
            false
        } finally {
            _isSaving.value = false
        }
    }

    suspend fun deleteEvent(event: Event): Boolean {
        if (_isSaving.value) return false
        _isSaving.value = true

        return try {
            // Move to remote trash first
            val isRepeating = event.repeat != RepeatType.NONE
            gitHubRepository.moveToRemoteTrash(event.filename, isRepeating)
            
            // Delete from local
            val success = eventRepository.deleteEvent(event)
            if (success) {
                eventRepository.loadEvents()
            }
            success
        } catch (e: Exception) {
            false
        } finally {
            _isSaving.value = false
        }
    }
}
