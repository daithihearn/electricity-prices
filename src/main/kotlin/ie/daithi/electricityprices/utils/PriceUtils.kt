package ie.daithi.electricityprices.utils

import RATING_VARIANCE
import VARIANCE_DIVISOR
import ie.daithi.electricityprices.model.DayRating
import ie.daithi.electricityprices.model.Price
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Joins prices that are adjacent to each other.
 */
fun joinPrices(cheapPrices: List<Price>): List<List<Price>> {
    val result = mutableListOf<List<Price>>()
    var i = 0
    while (i < cheapPrices.size) {
        val cheapPeriod = mutableListOf(cheapPrices[i])
        var j = i + 1
        while (j < cheapPrices.size && cheapPrices[j].dateTime == cheapPeriod.last().dateTime.plusHours(1)) {
            cheapPeriod.add(cheapPrices[j])
            j++
        }
        result.add(cheapPeriod)
        i = j
    }
    return result
}

/**
 * Calculate the combination between the thirty-day average and the daily average.
 * A weighted average is used with the daily average being weighted twice as much as the thirty-day average.
 */
private fun calculateCombinedAverage(dailyAverage: Double, thirtyDayAverage: Double): Double {
    return (dailyAverage * 2 + thirtyDayAverage) / 3
}

private fun calculateMinVariance(prices: List<Price>): Double {
    val cheapestPrice = prices.minByOrNull { it.price }?.price ?: return 0.0
    val expensivePrice = prices.maxByOrNull { it.price }?.price ?: return 0.0
    return (expensivePrice - cheapestPrice) / 6
}

private fun calculateMaxVariance(prices: List<Price>): Double {
    val cheapestPrice = prices.minByOrNull { it.price }?.price ?: return 0.0
    val expensivePrice = prices.maxByOrNull { it.price }?.price ?: return 0.0
    return (expensivePrice - cheapestPrice) / 3
}

/**
 * Calculate the variance for the cheap periods.
 * This is calculated as:
 * MAX(MIN((dailyAverage - cheapestPrice) / VARIANCE_DIVISOR, MAX_VARIANCE), MIN_VARIANCE)
 */
fun calculateCheapVariance(prices: List<Price>, thirtyDayAverage: Double): Double {
    val dailyAverage = calculateAverage(prices)
    val combinedAverage = calculateCombinedAverage(dailyAverage, thirtyDayAverage)
    val minVariance = calculateMinVariance(prices)
    val maxVariance = calculateMaxVariance(prices)
    val cheapestPrice = prices.minByOrNull { it.price }?.price ?: return minVariance
    val variance = (combinedAverage - cheapestPrice) / VARIANCE_DIVISOR

    return max(min(variance, maxVariance), minVariance)
}

/**
 * Calculate the variance for the expensive periods.
 * This is calculated as:
 * MAX(MIN((expensivePrice - dailyAverage) / VARIANCE_DIVISOR, MAX_VARIANCE), MIN_VARIANCE)
 */
fun calculateExpensiveVariance(prices: List<Price>, thirtyDayAverage: Double): Double {
    val dailyAverage = calculateAverage(prices)
    val combinedAverage = calculateCombinedAverage(dailyAverage, thirtyDayAverage)
    val minVariance = calculateMinVariance(prices)
    val maxVariance = calculateMaxVariance(prices)
    val expensivePrice = prices.maxByOrNull { it.price }?.price ?: return minVariance
    val variance = (expensivePrice - combinedAverage) / VARIANCE_DIVISOR

    return max(min(variance, maxVariance), minVariance)
}

/**
 * Check if a price is within the predefined variance of 0.02.
 * Returns true if the price is 0.02 less than the daily average or within 0.02 of the cheapest price.
 */
fun isWithinCheapPriceVariance(price: Double, cheapestPrice: Double, variance: Double): Boolean {
    return price - cheapestPrice <= variance
}

fun isWithinExpensivePriceVariance(
    price: Double,
    expensivePrice: Double,
    variance: Double
): Boolean {
    return expensivePrice - price <= variance
}

/**
 * Returns the cheap periods
 */
fun getCheapPeriods(
    prices: List<Price>,
    thirtyDayAverage: Double
): List<List<Price>> {

    val variance = calculateCheapVariance(prices, thirtyDayAverage)
    val cheapestPrice = prices.minByOrNull { it.price }?.price ?: return emptyList()

    val cheapPrices = prices.filter { price ->
        isWithinCheapPriceVariance(
            price.price,
            cheapestPrice,
            variance
        )
    }

    return joinPrices(cheapPrices)
}

/**
 * Returns the expensive periods
 */
fun getExpensivePeriods(
    prices: List<Price>,
    thirtyDayAverage: Double
): List<List<Price>> {

    val variance = calculateExpensiveVariance(prices, thirtyDayAverage)
    val expensivePrice = prices.maxByOrNull { it.price }?.price ?: return emptyList()

    // If the most expensive price would be considered cheap, return empty list
    if (expensivePrice < thirtyDayAverage - RATING_VARIANCE) return emptyList()

    val expensivePrices = prices.filter { price ->
        isWithinExpensivePriceVariance(
            price.price,
            expensivePrice,
            variance
        )
    }

    return joinPrices(expensivePrices)
}

fun calculateAverage(prices: List<Price>): Double {
    return prices.map { it.price }.average()
}

fun calculateAverageInCents(prices: List<Price>): Int {
    return calculateAverage(prices).times(100).roundToInt()
}


fun calculateRating(price: Double, thirtyDayAverage: Double): DayRating {
    val variance = price - thirtyDayAverage
    return when {
        variance < -RATING_VARIANCE -> DayRating.GOOD
        variance > RATING_VARIANCE -> DayRating.BAD
        else -> DayRating.NORMAL
    }
}
