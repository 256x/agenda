package fumi.day.literalagenda.data

import java.time.LocalDate
import java.time.LocalTime

data class Event(
    val filename: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val title: String,
    val note: String = "",
    val repeat: RepeatType = RepeatType.NONE
)

enum class RepeatType {
    NONE, WEEKLY, MONTHLY, YEARLY
}
