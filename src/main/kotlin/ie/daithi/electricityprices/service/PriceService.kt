package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.exceptions.DataNotAvailableYetException
import ie.daithi.electricityprices.model.*
import ie.daithi.electricityprices.repos.PriceRepo
import ie.daithi.electricityprices.utils.*
import org.apache.logging.log4j.LogManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PriceService(
    private val priceRepo: PriceRepo,
    private val reeRest: WebClient,
    private val esiosRest: WebClient,
    private val mongoTemplate: MongoTemplate
) {

    private val logger = LogManager.getLogger(this::class.simpleName)

    fun getTodayPrices(): List<Price> {
        logger.info("Getting today's prices")
        val now = LocalDate.now()
        return getPrices(now)
    }

    fun getPrices(date: LocalDate): List<Price> {
        logger.info("Getting prices for $date")
        val start = date.atStartOfDay().minusSeconds(1)
        val end = date.plusDays(1).atStartOfDay().minusSeconds(1)
        return getPrices(start, end)
    }

    fun getPrices(start: LocalDateTime, end: LocalDateTime): List<Price> {
        logger.info("Getting prices between $start and $end")
        return priceRepo.dateTimeBetween(start, end)
    }

    fun getTodayPriceInfo(): DailyPriceInfo {
        return getDailyPriceInfo(LocalDate.now())
    }

    fun getDailyPriceInfo(date: LocalDate): DailyPriceInfo {
        val prices = getPrices(date)
        if (prices.isEmpty()) throw DataNotAvailableYetException("Data is not available yet for $date")
        val thirtyDayAverage: Double = getThirtyDayAverage(date)
        val cheapestPeriods = getCheapPeriods(prices, thirtyDayAverage)
        val expensivePeriods = getExpensivePeriods(prices, thirtyDayAverage)

        val dailyAverage = calculateAverage(prices)

        return DailyPriceInfo(
            dayRating = calculateRating(dailyAverage, thirtyDayAverage),
            thirtyDayAverage = thirtyDayAverage,
            prices = prices,
            cheapestPeriods = cheapestPeriods,
            expensivePeriods = expensivePeriods
        )
    }

    fun calculateDailyMedians(prices: List<Price>): List<DailyMedian> {
        val medians = mutableListOf<DailyMedian>()
        val dates = prices.map { it.dateTime.toLocalDate() }.distinct()
        dates.forEach { date ->
            val pricesForDay = prices.filter { it.dateTime.toLocalDate() == date }
            val median = pricesForDay.map { it.price }.average()
            medians.add(DailyMedian(date, median))
        }
        return medians
    }

    fun getDailyMedians(date: LocalDate, numberOfDays: Long): List<DailyMedian> {
        val xDaysAgo = date.minusDays(numberOfDays).atStartOfDay().minusSeconds(1)
        val today = date.plusDays(1).atStartOfDay().minusSeconds(1)

        val prices = getPrices(xDaysAgo, today)
        if (prices.isEmpty()) throw DataNotAvailableYetException("30 day median data not available for $date")
        return calculateDailyMedians(prices)
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
                    dateTime = parseDateTimeFromOffset(it.datetime),
                    price = it.value / 1000
                )
            }

            logger.info("Saving ${prices?.size} prices for $dayStr from REE")
            priceRepo.saveAll(prices ?: emptyList())
        } else {
            logger.info("Price data for $dayStr is already up-to-date. REE sync not required.")
        }
    }

    fun getThirtyDayAverage(date: LocalDate): Double {
        val start = date.minusDays(30).atStartOfDay().minusSeconds(1)
        val end = date.plusDays(1).atStartOfDay().minusSeconds(1)

        // Stage 1: Filter documents within the desired date range
        val matchStage: MatchOperation = match(Criteria.where("dateTime").gte(start).lte(end))

        // Stage 2: Calculate the average price
        val groupStage = group().avg("price").`as`("averagePrice")

        // Define the aggregation pipeline
        val aggregation = newAggregation(matchStage, groupStage)

        // Execute the aggregation
        val result = mongoTemplate.aggregate(aggregation, "prices", AveragePriceResult::class.java)

        // Retrieve and return the average price
        return result.uniqueMappedResult?.averagePrice
            ?: throw DataNotAvailableYetException("30 day average data not available for $date")
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

data class AveragePriceResult(val averagePrice: Double)