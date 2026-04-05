package fumi.day.literalagenda.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ICalEvent(
    val summary: String,
    val date: LocalDate,
    val time: LocalTime?,
    val description: String,
    val repeat: RepeatType
)

object ICalImporter {

    fun parse(content: String): List<ICalEvent> {
        val events = mutableListOf<ICalEvent>()
        val lines = unfoldLines(content.lines())
        
        var inEvent = false
        var summary = ""
        var dtStart: String? = null
        var description = ""
        var rrule = ""

        for (line in lines) {
            when {
                line == "BEGIN:VEVENT" -> {
                    inEvent = true
                    summary = ""
                    dtStart = null
                    description = ""
                    rrule = ""
                }
                line == "END:VEVENT" -> {
                    if (inEvent && dtStart != null && summary.isNotBlank()) {
                        val (date, time) = parseDtStart(dtStart)
                        if (date != null) {
                            events.add(
                                ICalEvent(
                                    summary = summary,
                                    date = date,
                                    time = time,
                                    description = description,
                                    repeat = parseRRule(rrule)
                                )
                            )
                        }
                    }
                    inEvent = false
                }
                inEvent && line.startsWith("SUMMARY:") -> {
                    summary = line.removePrefix("SUMMARY:").trim()
                }
                inEvent && line.startsWith("DTSTART") -> {
                    dtStart = line.substringAfter(":").trim()
                }
                inEvent && line.startsWith("DESCRIPTION:") -> {
                    description = line.removePrefix("DESCRIPTION:")
                        .replace("\\n", "\n")
                        .replace("\\,", ",")
                        .trim()
                }
                inEvent && line.startsWith("RRULE:") -> {
                    rrule = line.removePrefix("RRULE:").trim()
                }
            }
        }

        return events
    }

    private fun unfoldLines(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                current.append(line.drop(1))
            } else {
                if (current.isNotEmpty()) {
                    result.add(current.toString())
                }
                current.clear()
                current.append(line)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        
        return result
    }

    private fun parseDtStart(value: String): Pair<LocalDate?, LocalTime?> {
        return try {
            when {
                value.contains("T") -> {
                    val clean = value.replace("Z", "")
                    val dt = LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                    dt.toLocalDate() to dt.toLocalTime()
                }
                value.length == 8 -> {
                    LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd")) to null
                }
                else -> null to null
            }
        } catch (e: Exception) {
            null to null
        }
    }

    private fun parseRRule(rrule: String): RepeatType {
        if (rrule.isBlank()) return RepeatType.NONE
        
        return when {
            rrule.contains("FREQ=WEEKLY") -> RepeatType.WEEKLY
            rrule.contains("FREQ=MONTHLY") -> RepeatType.MONTHLY
            rrule.contains("FREQ=YEARLY") -> RepeatType.YEARLY
            else -> RepeatType.NONE
        }
    }
}
