package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.DayRating
import ie.daithi.electricityprices.model.Price
import kotlin.math.roundToInt

const val RATING_VARIANCE = 0.02

// The maximum variance value
const val MAX_VARIANCE = 0.03

const val VARIANCE_DIVISOR = 2

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
 * Calculate the variance for the cheap periods.
 * This is calculated as:
 * MIN((dailyAverage - cheapestPrice) / VARIANCE_DIVISOR, MAX_VARIANCE)
 */
fun calculateCheapVariance(prices: List<Price>): Double {
    val dailyAverage = calculateAverage(prices)
    val cheapestPrice = prices.minByOrNull { it.price }?.price ?: return MAX_VARIANCE
    val variance = (dailyAverage - cheapestPrice) / VARIANCE_DIVISOR
    return if (variance > MAX_VARIANCE) MAX_VARIANCE else variance
}

/**
 * Calculate the variance for the expensive periods.
 * This is calculated as:
 * MIN((expensivePrice - dailyAverage) / VARIANCE_DIVISOR, MAX_VARIANCE)
 */
fun calculateExpensiveVariance(prices: List<Price>): Double {
    val dailyAverage = calculateAverage(prices)
    val expensivePrice = prices.maxByOrNull { it.price }?.price ?: return MAX_VARIANCE
    val variance = (expensivePrice - dailyAverage) / VARIANCE_DIVISOR
    return if (variance > MAX_VARIANCE) MAX_VARIANCE else variance
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
    prices: List<Price>
): List<List<Price>> {

    val variance = calculateCheapVariance(prices)
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
    prices: List<Price>
): List<List<Price>> {

    val variance = calculateExpensiveVariance(prices)
    val expensivePrice = prices.maxByOrNull { it.price }?.price ?: return emptyList()

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
