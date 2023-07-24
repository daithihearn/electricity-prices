package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.Price

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

    val remainingPricesBefore = prices.filter { it.dateTime.isBefore(firstPeriod[0].dateTime) }
    val remainingPricesAfter = prices.filter { it.dateTime.isAfter(firstPeriod[n - 1].dateTime) }

    val firstPeriodBefore = getCheapestPeriod(remainingPricesBefore, n)
    val firstPeriodAfter = getCheapestPeriod(remainingPricesAfter, n)

    var secondPeriod = emptyList<Price>()

    secondPeriod = if (firstPeriodBefore.size == n && firstPeriodAfter.size == n) {
        val firstPeriodBeforeAverage = calculateAverage(firstPeriodBefore)
        val firstPeriodAfterAverage = calculateAverage(firstPeriodAfter)

        if (firstPeriodBeforeAverage < firstPeriodAfterAverage) firstPeriodBefore else firstPeriodAfter
    } else {
        if (firstPeriodBefore.size == n) firstPeriodBefore else firstPeriodAfter
    }

    val firstPeriodAverage = calculateAverage(firstPeriod)
    val secondPeriodAverage = calculateAverage(secondPeriod)

    if (kotlin.math.abs(firstPeriodAverage - secondPeriodAverage) > VARIANCE) {
        secondPeriod = emptyList()
    }

    // If the second period is empty or outside the variance return the first period
    return if (secondPeriod.isEmpty() || kotlin.math.abs(firstPeriodAverage - secondPeriodAverage) > VARIANCE) {
        Pair(firstPeriod, emptyList())
    } else {
        if (firstPeriod[0].dateTime.isBefore(secondPeriod[0].dateTime)) Pair(firstPeriod, secondPeriod) else Pair(secondPeriod, firstPeriod)
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
