package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.Price

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

