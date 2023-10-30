package ie.daithi.electricityprices.model

import java.time.LocalDate

data class DailyAverage(
    val date: LocalDate,
    val average: Double
)
