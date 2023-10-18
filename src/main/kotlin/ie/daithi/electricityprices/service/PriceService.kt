package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.exceptions.DataNotAvailableYetException
import ie.daithi.electricityprices.model.DailyPriceInfo
import ie.daithi.electricityprices.model.EsiosPrice
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.model.ReePrice
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
    private val reeRest: WebClient,
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
        val endDate =
            (end?.let { LocalDate.parse(it, dateFormatter) } ?: today).plusDays(1).atStartOfDay().minusSeconds(1)

        logger.info("Getting prices between $startDate and $endDate")
        return priceRepo.dateTimeBetween(startDate, endDate)
    }

    fun getDailyPriceInfo(dateStr: String?): DailyPriceInfo? {
        val date = dateStr?.let { LocalDate.parse(it, dateFormatter) } ?: LocalDate.now()
        val prices = getPrices(start = dateStr, end = dateStr)
        if (prices.isEmpty()) return null
        val thirtyDayAverage: Double = getThirtyDayAverage(date.atStartOfDay())
        val cheapestPeriods = getCheapPeriods(prices, thirtyDayAverage)
        val expensivePeriods = getExpensivePeriods(prices, thirtyDayAverage)

        val dailyAverage = prices.map { it.price }.average()

        return DailyPriceInfo(
            dayRating = calculateRating(dailyAverage, thirtyDayAverage),
            thirtyDayAverage = thirtyDayAverage,
            prices = prices,
            cheapestPeriods = cheapestPeriods,
            expensivePeriods = expensivePeriods
        )
    }

    /*
        Calls to the API and update the latest prices
        We use the REE API as they have the most up-to-date data
     */
    fun updatePriceData(date: LocalDate) {
        logger.info("Updating price data from REE for $date")

        val previousDay = date.minusDays(1)

        // Get the prices for tomorrow
        val currPrices = getPrices(date)

        val dayStr = date.format(dateFormatter)

        // Validate that we have prices for the day
        if (!validatePricesForDay(currPrices, date)) {
            val reePrices = reeRest.get()
                .uri(
                    "?time_trunc=hour&start_date=${previousDay.format(dateFormatter)}T00:00&end_date=${dayStr}T23:59"
                )
                .retrieve().bodyToMono(ReePrice::class.java).block()
            if (reePrices == null || reePrices.included.isEmpty())
                throw DataNotAvailableYetException("Data is not available yet for $dayStr on REE")

            val pvpc = reePrices.included.find { it.id == "1001" }
            val prices = pvpc?.attributes?.values?.map {
                Price(
                    dateTime = LocalDateTime.parse(it.datetime, dateTimeOffsetFormatter),
                    price = it.value / 1000
                )
            }

            logger.info("Saving ${prices?.size} prices for $dayStr from REE")
            priceRepo.saveAll(prices ?: emptyList())
        } else {
            logger.info("Price data for $dayStr is already up-to-date. REE sync not required.")
        }
    }

    fun getThirtyDayAverage(date: LocalDateTime? = LocalDateTime.now()): Double {
        return getPrices(start = date?.toLocalDate()?.minusDays(30)?.format(dateFormatter), end = null)
            .map { it.price }.average()
    }

    /*
        Syncs the prices for the given day with ESIOS API
        We use ESIOS API as they have all historical data
     */
    fun syncEsiosData(day: LocalDate) {
        // Get the prices for the day
        val pricesToday = getPrices(day)

        // Validate that we have prices for the day
        if (!validatePricesForDay(pricesToday, day)) {
            logger.info("Failed to validate prices for $day. Will attempt to sync with ESIOS")

            // Clear the prices for the day
            priceRepo.deleteAll(pricesToday)

            // Get the prices from ESIOS
            val dayStr = day.format(dateFormatter)
            val query = "?date=$dayStr"
            val esiosPrices = esiosRest.get().uri(query).retrieve().bodyToMono(EsiosPrice::class.java).block()
            if ((esiosPrices == null || esiosPrices.pvpc.isNullOrEmpty()) && day.isAfter(LocalDate.now()))
                throw DataNotAvailableYetException("Data is not available yet for $dayStr on ESIOS")

            logger.info("Saving ${esiosPrices?.pvpc?.size} prices for $dayStr from ESIOS")
            val prices = esiosPrices?.pvpc?.map {
                Price(
                    dateTime = LocalDateTime.parse("${it.day}${it.hour.substring(0, 2)}:00:00", esiosFormatter),
                    price = esNumberFormat.parse(it.pcb ?: it.gen).toDouble().div(1000)
                )
            }

            if (!prices.isNullOrEmpty()) priceRepo.saveAll(prices)
        } else {
            logger.info("Price data for $day is already up-to-date. ESIOS sync not required.")
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
