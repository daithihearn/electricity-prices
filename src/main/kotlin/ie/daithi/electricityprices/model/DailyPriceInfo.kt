package ie.daithi.electricityprices.model

data class DailyPriceInfo(
    val dayRating: DayRating,
    val thirtyDayAverage: Double,
    val prices: List<Price>,
    val cheapestPeriods: List<List<Price>>,
    val expensivePeriods: List<List<Price>>,
)
