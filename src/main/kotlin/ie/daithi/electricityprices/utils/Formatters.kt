package ie.daithi.electricityprices.utils

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

val esLocale = Locale.Builder().setLanguage("es").setRegion("ES").build()
val esNumberFormat = NumberFormat.getNumberInstance(esLocale)

val esiosQueryFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", esLocale)
val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", esLocale)
val dateTimeOffsetFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyyHH:mm:ss", esLocale)