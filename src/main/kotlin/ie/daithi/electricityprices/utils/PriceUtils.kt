package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.DayRating
import ie.daithi.electricityprices.model.Price
import kotlin.math.abs

const val VARIANCE = 0.02

fun getCheapestPeriod(prices: List<Price>, n: Int): List<Price> {
    if (prices.size < n) {
        return emptyList()
    }

    val pricesSorted = prices.sortedBy { it.dateTime }

    var minSum = Double.POSITIVE_INFINITY
    var minWindow = emptyList<Price>()

    for (i in 0 until pricesSorted.size - n + 1) {
        val window = pricesSorted.subList(i, i + n)
        val windowSum = window.sumOf { it.price }

        if (windowSum < minSum) {
            minSum = windowSum
            minWindow = window
        }
    }

    return minWindow
}

fun getTwoCheapestPeriods(prices: List<Price>, n: Int): Pair<List<Price>, List<Price>> {
    if (prices.size < n) {
        return Pair(emptyList(), emptyList())
    }

    val firstPeriod = getCheapestPeriod(prices, n)

    val remainingPricesBefore = prices.filter { it.dateTime.isBefore(firstPeriod.first().dateTime) }
    val remainingPricesAfter = prices.filter { it.dateTime.isAfter(firstPeriod.last().dateTime) }

    val firstPeriodBefore = getCheapestPeriod(remainingPricesBefore, n)
    val firstPeriodAfter = getCheapestPeriod(remainingPricesAfter, n)

    var secondPeriod: List<Price> = emptyList()

    if (firstPeriodBefore.size == n && firstPeriodAfter.size == n) {
        val firstPeriodBeforeAverage = calculateAverage(firstPeriodBefore)
        val firstPeriodAfterAverage = calculateAverage(firstPeriodAfter)

        secondPeriod = if (firstPeriodBeforeAverage < firstPeriodAfterAverage) firstPeriodBefore else firstPeriodAfter
    } else {
        secondPeriod = if (firstPeriodBefore.size == n) firstPeriodBefore else firstPeriodAfter
    }

    if (abs(calculateAverage(firstPeriod) - calculateAverage(secondPeriod)) > VARIANCE) {
        secondPeriod = emptyList()
    }

    return when {
        secondPeriod.isEmpty() || abs(calculateAverage(firstPeriod) - calculateAverage(secondPeriod)) > VARIANCE -> Pair(
            firstPeriod,
            emptyList()
        )

        firstPeriod.last().dateTime == secondPeriod.first().dateTime.minusHours(1) -> Pair(
            firstPeriod + secondPeriod,
            emptyList()
        )

        firstPeriod.first().dateTime == secondPeriod.last().dateTime.plusHours(1) -> Pair(
            secondPeriod + firstPeriod,
            emptyList()
        )

        firstPeriod.first().dateTime.isBefore(secondPeriod.first().dateTime) -> Pair(firstPeriod, secondPeriod)
        else -> Pair(secondPeriod, firstPeriod)
    }
}

fun getMostExpensivePeriod(prices: List<Price>, n: Int): List<Price> {
    if (prices.size < n) {
        return emptyList()
    }

    val pricesSorted = prices.sortedBy { it.dateTime }

    var maxSum = Double.NEGATIVE_INFINITY
    var maxWindow = emptyList<Price>()

    for (i in 0 until pricesSorted.size - n + 1) {
        val window = pricesSorted.subList(i, i + n)
        val windowSum = window.sumOf { it.price }

        if (windowSum > maxSum) {
            maxSum = windowSum
            maxWindow = window
        }
    }

    return maxWindow
}

fun calculateAverage(prices: List<Price>): Double {
    return prices.map { it.price }.average()
}


fun calculateRating(price: Double, thirtyDayAverage: Double): DayRating {
    val variance = price - thirtyDayAverage
    return when {
        variance < -VARIANCE -> DayRating.GOOD
        variance > VARIANCE -> DayRating.BAD
        else -> DayRating.NORMAL
    }
}