package fumi.day.literalagenda.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val eventsDir: File
        get() = File(context.filesDir, "events").also { it.mkdirs() }

    private val repeatingDir: File
        get() = File(context.filesDir, "repeating").also { it.mkdirs() }

private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: Flow<List<Event>> = _events

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    suspend fun loadEvents() {
        withContext(Dispatchers.IO) {
            val singleEvents = eventsDir.listFiles()
                ?.filter { it.extension == "md" }
                ?.mapNotNull { parseEventFile(it, false) }
                ?: emptyList()

            val repeatingEvents = repeatingDir.listFiles()
                ?.filter { it.extension == "md" }
                ?.mapNotNull { parseEventFile(it, true) }
                ?: emptyList()

            _events.value = (singleEvents + repeatingEvents).sortedBy { it.date }
        }
    }

    fun getUpcomingEvents(months: Int = 3): Flow<List<Event>> {
        val today = LocalDate.now()
        val endDate = today.plusMonths(months.toLong())

        return _events.map { events ->
            val expanded = mutableListOf<Event>()

            events.forEach { event ->
                when (event.repeat) {
                    RepeatType.NONE -> {
                        if (!event.date.isBefore(today) && !event.date.isAfter(endDate)) {
                            expanded.add(event)
                        }
                    }
                    RepeatType.WEEKLY -> {
                        var d = event.date
                        while (!d.isAfter(endDate)) {
                            if (!d.isBefore(today)) {
                                expanded.add(event.copy(date = d))
                            }
                            d = d.plusWeeks(1)
                        }
                    }
                    RepeatType.MONTHLY -> {
                        var d = event.date
                        while (!d.isAfter(endDate)) {
                            if (!d.isBefore(today)) {
                                expanded.add(event.copy(date = d))
                            }
                            d = d.plusMonths(1)
                        }
                    }
                    RepeatType.YEARLY -> {
                        var d = event.date
                        while (!d.isAfter(endDate)) {
                            if (!d.isBefore(today)) {
                                expanded.add(event.copy(date = d))
                            }
                            d = d.plusYears(1)
                        }
                    }
                }
            }

            expanded.sortedWith(compareBy({ it.date }, { it.time }))
        }
    }

    suspend fun saveEvent(event: Event): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dir = if (event.repeat == RepeatType.NONE) eventsDir else repeatingDir
                val file = File(dir, event.filename)
                file.writeText(eventToMarkdown(event))
                loadEvents()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun createEvent(
        date: LocalDate,
        time: LocalTime?,
        title: String,
        note: String,
        repeat: RepeatType
    ): Boolean {
        val filename = dateFormatter.format(java.time.LocalDateTime.now()) + ".md"
        val event = Event(filename, date, time, title, note, repeat)
        return saveEvent(event)
    }

    suspend fun deleteEvent(event: Event): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dir = if (event.repeat == RepeatType.NONE) eventsDir else repeatingDir
                val file = File(dir, event.filename)
                val deleted = file.delete()
                if (deleted) loadEvents()
                deleted
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun importFromICal(inputStream: InputStream): Int {
        return withContext(Dispatchers.IO) {
            try {
                val content = inputStream.bufferedReader().readText()
                val icalEvents = ICalImporter.parse(content)
                var count = 0

                for (icalEvent in icalEvents) {
                    val filename = dateFormatter.format(java.time.LocalDateTime.now()) + "_${count}.md"
                    val event = Event(
                        filename = filename,
                        date = icalEvent.date,
                        time = icalEvent.time,
                        title = icalEvent.summary,
                        note = icalEvent.description,
                        repeat = icalEvent.repeat
                    )
                    val dir = if (event.repeat == RepeatType.NONE) eventsDir else repeatingDir
                    val file = File(dir, event.filename)
                    file.writeText(eventToMarkdown(event))
                    count++
                    // Small delay to ensure unique filenames
                    Thread.sleep(10)
                }

                loadEvents()
                count
            } catch (e: Exception) {
                0
            }
        }
    }

    fun searchEvents(query: String): Flow<List<Event>> {
        val lowerQuery = query.lowercase()
        return _events.map { events ->
            events.filter {
                it.title.lowercase().contains(lowerQuery) ||
                        it.note.lowercase().contains(lowerQuery)
            }
        }
    }

    private fun parseEventFile(file: File, isRepeating: Boolean): Event? {
        return try {
            val content = file.readText()
            val lines = content.lines()

            if (lines.firstOrNull()?.trim() != "---") return null

            val frontmatterEnd = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (frontmatterEnd < 0) return null

            val frontmatter = lines.subList(1, frontmatterEnd + 1)
            val body = lines.drop(frontmatterEnd + 2)

            var date: LocalDate? = null
            var time: LocalTime? = null
            var repeat = if (isRepeating) RepeatType.WEEKLY else RepeatType.NONE

            frontmatter.forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    when (key) {
                        "date" -> date = LocalDate.parse(value)
                        "time" -> time = if (value.isNotBlank()) LocalTime.parse(value) else null
                        "repeat" -> repeat = RepeatType.entries.find {
                            it.name.equals(value, ignoreCase = true)
                        } ?: RepeatType.NONE
                    }
                }
            }

            val title = body.firstOrNull { it.isNotBlank() } ?: return null
            val note = body.drop(1).joinToString("\n").trim()

            Event(
                filename = file.name,
                date = date ?: return null,
                time = time,
                title = title,
                note = note,
                repeat = repeat
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun eventToMarkdown(event: Event): String {
        return buildString {
            appendLine("---")
            appendLine("date: ${event.date}")
            event.time?.let { appendLine("time: $it") }
            appendLine("repeat: ${event.repeat.name.lowercase()}")
            appendLine("---")
            appendLine(event.title)
            if (event.note.isNotBlank()) {
                appendLine(event.note)
            }
        }
    }
}
