package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.exceptions.DataNotAvailableYetException
import ie.daithi.electricityprices.model.EsiosPrice
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.repos.PriceRepo
import ie.daithi.electricityprices.utils.*
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PriceService(
        private val priceRepo: PriceRepo,
        private val esiosRest: WebClient
) {

    private val logger = LogManager.getLogger(this::class.simpleName)

    fun getPrices(date: LocalDate): List<Price> {
        logger.info("Getting prices for $date")
        val startDate = date.atStartOfDay().minusSeconds(1)
        val endDate = date.plusDays(1).atStartOfDay().minusSeconds(1)
        return priceRepo.dateTimeBetween(startDate, endDate)
    }

    fun getPrices(start: String?, end: String?): List<Price> {
        val today = LocalDate.now()
        val startDate = (start?.let { LocalDate.parse(it, dateFormatter) } ?: today).atStartOfDay().minusSeconds(1)
        val endDate = (end?.let { LocalDate.parse(it, dateFormatter) } ?: today).plusDays(1).atStartOfDay().minusSeconds(1)

        logger.info("Getting prices between $startDate and $endDate")
        return priceRepo.dateTimeBetween(startDate, endDate)
    }

    fun syncEsiosData(day: LocalDate) {
        // Get the prices for the day
        val prices = getPrices(day)

        // Validate that we have prices for the day
        if (!validatePricesForDay(prices, day)) {
            logger.info("Failed to validate prices for $day. Will attempt to sync with ESIOS")

            // Clear the prices for the day
            priceRepo.deleteAll(prices)

            // Get the prices from ESIOS
            val query = "?date=${day.format(esiosQueryFormatter)}"
            val esiosPrices = esiosRest.get().uri(query).retrieve().bodyToMono(EsiosPrice::class.java).block()
            if ((esiosPrices == null || esiosPrices.pvpc.isNullOrEmpty()) && day.isEqual(LocalDate.now().plusDays(1)))
                throw DataNotAvailableYetException("Tomorrow's data is not available yet")

            logger.info("Received ${esiosPrices?.pvpc?.size} prices from ESIOS")
            val prices = esiosPrices?.pvpc?.map {
                Price(
                dateTime = LocalDateTime.parse("${it.day}${it.hour.substring(0,2)}:00:00", dateTimeOffsetFormatter),
                price = esNumberFormat.parse(it.pcb ?: it.gen).toDouble().div(1000)
            ) }

            if (!prices.isNullOrEmpty()) priceRepo.saveAll(prices)
        } else {
            logger.info("Existing prices for $day are valid")
        }
    }

    /*
     * Validate that we have prices for every hour of the day
     */
    private fun validatePricesForDay(prices: List<Price>, day: LocalDate): Boolean {
        if (prices.size != 24) {
            logger.info("Missing prices for $day. Only found ${prices.size}")
            return false
        }

        val hours = prices.map { it.dateTime.hour }
        val missingHours = (0..23).filter { !hours.contains(it) }
        if (missingHours.isNotEmpty()) {
            logger.info("Missing prices for $day: $missingHours")
            return false
        }
        return true
    }
}
