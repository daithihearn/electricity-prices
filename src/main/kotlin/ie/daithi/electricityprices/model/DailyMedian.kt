package ie.daithi.electricityprices.model

import java.time.LocalDate

data class DailyMedian(
    val date: LocalDate,
    val median: Double
)
