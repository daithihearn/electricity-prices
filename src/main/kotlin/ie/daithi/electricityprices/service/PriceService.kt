package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.model.ReePrice
import ie.daithi.electricityprices.repos.PriceRepo
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

@Service
class PriceService(
        private val priceRepo: PriceRepo,
        private val reeRest: WebClient
) {

    private val logger = LogManager.getLogger(this::class.simpleName)

    fun getPrices(start: String?, end: String?): List<Price> {
        val today = LocalDate.now()
        val startDate = start?.let { LocalDateTime.parse(it, dateTimeFormatter) } ?: today.atStartOfDay().minusSeconds(1)
        val endDate = end?.let { LocalDateTime.parse(it, dateTimeFormatter) } ?: today.plusDays(1).atStartOfDay()

        logger.info("Getting prices between $startDate and $endDate")
        return priceRepo.dateTimeBetween(startDate, endDate)
    }

    // Calls to the API and update the latest prices
    fun updatePriceData() {
        logger.info("Updating price data")
        val today = LocalDate.now().format(dateFormatter)
        val yesterday = LocalDate.now().minusDays(1).format(dateFormatter)
        val reePrices = reeRest.get().uri("?time_trunc=hour&start_date=${yesterday}T00:00&end_date=${today}T23:59").retrieve().bodyToMono(ReePrice::class.java).block()

        val pvpc = reePrices?.included?.find { it.id == "1001" }
        val prices = pvpc?.attributes?.values?.map { Price(
                dateTime = LocalDateTime.parse(it.datetime, dateTimeFormatter),
                price = it.value / 1000
            ) }

        logger.info("Saving ${prices?.size} prices")
        priceRepo.saveAll(prices ?: emptyList())
    }
}