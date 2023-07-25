package ie.daithi.electricityprices.utils

import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

val esLocale = Locale.Builder().setLanguage("es").setRegion("ES").build()
val esNumberFormat = NumberFormat.getNumberInstance(esLocale)

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", esLocale)
val esiosFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyyHH:mm:ss", esLocale)
val dateTimeOffsetFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", esLocale)
val alexaSkillFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'", esLocale)
val amPmFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h a", esLocale)

fun formatAmPm(dateTime: LocalDateTime): String {
    return amPmFormatter.format(dateTime).replace("a. m.", "AM")
        .replace("p. m.", "PM")
}