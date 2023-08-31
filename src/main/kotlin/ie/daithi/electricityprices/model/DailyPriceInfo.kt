package ie.daithi.electricityprices.model

data class DailyPriceInfo(
    val dayRating: DayRating,
    val prices: List<Price>,
    val cheapestPeriods: Pair<List<Price>, List<Price>>,
    val expensivePeriod: List<Price>,
)
