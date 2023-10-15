package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.DayRating
import ie.daithi.electricityprices.model.Price
import kotlin.math.abs

const val VARIANCE = 0.02

/**
 * Returns the cheapest period. The period is calculated by getting the cheapest hour and then selecting
 * adjacent hours that fall within a price variance of 0.02.
 */
fun getCheapestPeriod(prices: List<Price>): List<Price> {
    val cheapestHour = prices.minByOrNull { it.price } ?: return emptyList()

    val cheapestPeriod = mutableListOf(cheapestHour)

    var i = prices.indexOf(cheapestHour) + 1
    while (i < prices.size && abs(prices[i].price - cheapestHour.price) < VARIANCE) {
        cheapestPeriod.add(prices[i])
        i++
    }

    i = prices.indexOf(cheapestHour) - 1
    while (i >= 0 && abs(prices[i].price - cheapestHour.price) < VARIANCE) {
        cheapestPeriod.add(prices[i])
        i--
    }

    return cheapestPeriod.sortedBy { it.dateTime }
}

/**
 * Returns the cheapest period of n hours.
 * If there are multiple periods with the same price, the first is returned.
 * If there are less than n prices, an empty list is returned.
 */
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

/**
 * Returns the two cheapest periods.
 * If there is only one period that falls within the predefined variance of 0.02, the second period is empty.
 */
fun getTwoCheapestPeriods(prices: List<Price>): Pair<List<Price>, List<Price>> {
    if (prices.size < 2) {
        return Pair(emptyList(), emptyList())
    }

    val firstPeriod = getCheapestPeriod(prices)

    val remainingPricesBefore = prices.filter { it.dateTime.isBefore(firstPeriod.first().dateTime) }.dropLast(1)
    val remainingPricesAfter = prices.filter { it.dateTime.isAfter(firstPeriod.last().dateTime) }.drop(1)

    val firstPeriodBefore = getCheapestPeriod(remainingPricesBefore)
    val firstPeriodAfter = getCheapestPeriod(remainingPricesAfter)

    var secondPeriod: List<Price>

    secondPeriod = if (firstPeriodBefore.isNotEmpty() && firstPeriodAfter.isNotEmpty()) {
        val firstPeriodBeforeAverage = calculateAverage(firstPeriodBefore)
        val firstPeriodAfterAverage = calculateAverage(firstPeriodAfter)

        if (firstPeriodBeforeAverage < firstPeriodAfterAverage) firstPeriodBefore else firstPeriodAfter
    } else if (firstPeriodBefore.isNotEmpty()) {
        firstPeriodBefore
    } else if (firstPeriodAfter.isNotEmpty()) {
        firstPeriodAfter
    } else {
        emptyList()
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

/**
 * Returns the two cheapest periods of n hours.
 * If there is only one period that falls within the predefined variance of 0.02, the second period is empty.
 */
fun getTwoCheapestPeriods(prices: List<Price>, n: Int): Pair<List<Price>, List<Price>> {
    if (prices.size < n) {
        return Pair(emptyList(), emptyList())
    }

    val firstPeriod = getCheapestPeriod(prices, n)

    val remainingPricesBefore = prices.filter { it.dateTime.isBefore(firstPeriod.first().dateTime) }
    val remainingPricesAfter = prices.filter { it.dateTime.isAfter(firstPeriod.last().dateTime) }

    val firstPeriodBefore = getCheapestPeriod(remainingPricesBefore, n)
    val firstPeriodAfter = getCheapestPeriod(remainingPricesAfter, n)

    var secondPeriod: List<Price>

    secondPeriod = if (firstPeriodBefore.size == n && firstPeriodAfter.size == n) {
        val firstPeriodBeforeAverage = calculateAverage(firstPeriodBefore)
        val firstPeriodAfterAverage = calculateAverage(firstPeriodAfter)

        if (firstPeriodBeforeAverage < firstPeriodAfterAverage) firstPeriodBefore else firstPeriodAfter
    } else {
        if (firstPeriodBefore.size == n) firstPeriodBefore else firstPeriodAfter
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

/**
 * Returns the most expensive period.
 * The period is calculated by getting the most expensive hour and then selecting
 * adjacent hours that fall within a price variance of 0.02.
 */
fun getMostExpensivePeriod(prices: List<Price>): List<Price> {
    val mostExpensiveHour = prices.maxByOrNull { it.price } ?: return emptyList()

    val mostExpensivePeriod = mutableListOf(mostExpensiveHour)

    var i = prices.indexOf(mostExpensiveHour) + 1
    while (i < prices.size && abs(prices[i].price - mostExpensiveHour.price) < VARIANCE) {
        mostExpensivePeriod.add(prices[i])
        i++
    }

    i = prices.indexOf(mostExpensiveHour) - 1
    while (i >= 0 && abs(prices[i].price - mostExpensiveHour.price) < VARIANCE) {
        mostExpensivePeriod.add(prices[i])
        i--
    }

    return mostExpensivePeriod.sortedBy { it.dateTime }
}

/**
 * Returns the most expensive period of n hours.
 */
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