package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.AlexaSkillResponse
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.utils.*
import org.apache.logging.log4j.LogManager
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.math.roundToInt

@Service
class AlexSkillService(private val priceSerice: PriceService, private val messageSource: MessageSource) {

    private val logger = LogManager.getLogger(this::class.simpleName)

    fun getResponses(locale: Locale): List<AlexaSkillResponse> {
        val responses = mutableListOf<AlexaSkillResponse>()

        // Get Today's price data
        val now = LocalDateTime.now()
        val pricesToday = priceSerice.getPrices(now.toLocalDate())

        if (pricesToday.isEmpty()) {
            return responses
        }

        // Current Price
        responses.add(getCurrentPrice(pricesToday, now, locale))

        // Today Rating
        val thirtyDayAverage = priceSerice.getPrices(start = now.toLocalDate().minusDays(30).format(dateFormatter), end = null).map { it.price }.average()
        responses.add(getTodayRating(pricesToday, now, thirtyDayAverage, locale))

        // Get next good 3-hour period
        val nextCheapPeriod = getNextCheapPeriod(pricesToday, now, locale)
        nextCheapPeriod?.let { responses.add(it) }

        // Get next bad 3-hour period
        val nextExpensivePeriod = getNextExpensivePeriod(pricesToday, now, locale)
        nextExpensivePeriod?.let { responses.add(it) }

        // Get Tomorrow's price data
        val tomorrow = now.plusDays(1)
        val pricesTomorrow = priceSerice.getPrices(tomorrow.toLocalDate())

        if (pricesTomorrow.isNotEmpty()) {
            // Tomorrow Rating
            responses.add(getTomorrowRating(pricesTomorrow, tomorrow, thirtyDayAverage, locale))
        }

        return responses
    }

    private fun getNextCheapPeriod(pricesToday: List<Price>, now: LocalDateTime, locale: Locale): AlexaSkillResponse? {
        val twoCheapestPeriods = getTwoCheapestPeriods(pricesToday, 3)

        val nextPeriod = if (twoCheapestPeriods.first.isNotEmpty() && !twoCheapestPeriods.first[2].dateTime.plusMinutes(59).isBefore(now)) {
            twoCheapestPeriods.first
        } else if (twoCheapestPeriods.second.isNotEmpty() && !twoCheapestPeriods.second[2].dateTime.plusMinutes(59).isBefore(now)) {
            twoCheapestPeriods.second
        } else {
            return null
        }

        // Get average price for period
        val averagePrice = nextPeriod.map { it.price }.average().times(100).roundToInt()

        val cheapestPeriodTime = amPmFormatter.format(nextPeriod[0].dateTime)

        // If period hasn't started send message
        return if (nextPeriod[0].dateTime.isAfter(now)) {
            AlexaSkillResponse(
                updateDate = now.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.next.cheap.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage("alexa.next.cheap.period.main", arrayOf(cheapestPeriodTime, averagePrice), locale)
            )
        } else {
            // We are currently in the good period
            AlexaSkillResponse(
                updateDate = now.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.current.cheap.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage("alexa.current.cheap.period.main", arrayOf(cheapestPeriodTime, averagePrice), locale)
            )
        }
    }

    fun getNextExpensivePeriod(pricesToday: List<Price>, now: LocalDateTime, locale: Locale): AlexaSkillResponse? {
        val expensivePeriod = getMostExpensivePeriod(pricesToday, 3)

        // If the period has passed do nothing
        if (expensivePeriod[2].dateTime.plusMinutes(59).isBefore(now)) {
            return null
        }

        // Get average price for period
        val averagePrice = expensivePeriod.map { it.price }.average().times(100).roundToInt()

        val expensivePeriodTime = amPmFormatter.format(expensivePeriod[0].dateTime)

        // If period hasn't started send message
        return if (expensivePeriod[0].dateTime.isAfter(now)) {
            AlexaSkillResponse(
                updateDate = now.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.next.expensive.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage("alexa.next.expensive.period.main", arrayOf(expensivePeriodTime, averagePrice), locale)
            )
        } else {
            // We are currently in the good period
            AlexaSkillResponse(
                updateDate = now.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.current.expensive.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage("alexa.current.expensive.period.main", arrayOf(expensivePeriodTime, averagePrice), locale)
            )
        }
    }

    /**
     * Get the current price
     */
    fun getCurrentPrice(prices: List<Price>, dateTime: LocalDateTime, locale: Locale): AlexaSkillResponse {

        // Get current price
        val currentPrice = prices.find { it.dateTime.hour == dateTime.hour }

        // Round current price to nearest cent
        val currentPriceCents = currentPrice?.price?.times(100)?.roundToInt()

        return AlexaSkillResponse(
            updateDate = dateTime.format(alexaSkillFormatter),
            titleText = messageSource.getMessage("alexa.current.price.title", emptyArray(), locale),
            mainText = messageSource.getMessage("alexa.current.price.main", arrayOf(currentPriceCents), locale)
        )
    }

    fun getTodayRating(prices: List<Price>, dateTime: LocalDateTime, thirtyDayAverage: Double, locale: Locale): AlexaSkillResponse {
        val dailyAverage = prices.map { it.price }.average()

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        val mainText = when {
            dailyAverage > thirtyDayAverage + 2 -> messageSource.getMessage("alexa.today.rating.main.good", arrayOf(roundedDailyAverage), locale)
            dailyAverage < thirtyDayAverage - 2 -> messageSource.getMessage("alexa.today.rating.main.bad", arrayOf(roundedDailyAverage), locale)
            else -> messageSource.getMessage("alexa.today.rating.main.normal", arrayOf(roundedDailyAverage), locale)
        }

        return AlexaSkillResponse(
            updateDate = dateTime.format(alexaSkillFormatter),
            titleText = messageSource.getMessage("alexa.today.rating.title", emptyArray(), locale),
            mainText = mainText
        )
    }

    fun getTomorrowRating(prices: List<Price>, dateTime: LocalDateTime, thirtyDayAverage: Double, locale: Locale): AlexaSkillResponse {
        val dailyAverage = prices.map { it.price }.average()

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        // Get cheapest period
        val cheapestPeriod = getCheapestPeriod(prices, 3)
        val cheapestPeriodAverage = cheapestPeriod.map { it.price }.average().times(100).roundToInt()
        val cheapestPeriodTime = amPmFormatter.format(cheapestPeriod[0].dateTime)

        // Get most expensive period
        val mostExpensivePeriod = getCheapestPeriod(prices, 3)
        val mostExpensivePeriodAverage = mostExpensivePeriod.map { it.price }.average().times(100).roundToInt()
        val mostExpensivePeriodTime = amPmFormatter.format(mostExpensivePeriod[0].dateTime)

        val mainText = when {
            dailyAverage > thirtyDayAverage + 2 -> messageSource.getMessage("alexa.tomorrow.rating.main.good", arrayOf(roundedDailyAverage, cheapestPeriodTime, cheapestPeriodAverage, mostExpensivePeriodTime, mostExpensivePeriodAverage), locale)
            dailyAverage < thirtyDayAverage - 2 -> messageSource.getMessage("alexa.tomorrow.rating.main.bad", arrayOf(roundedDailyAverage, cheapestPeriodTime, cheapestPeriodAverage, mostExpensivePeriodTime, mostExpensivePeriodAverage), locale)
            else -> messageSource.getMessage("alexa.tomorrow.rating.main.normal", arrayOf(roundedDailyAverage, cheapestPeriodTime, cheapestPeriodAverage, mostExpensivePeriodTime, mostExpensivePeriodAverage), locale)
        }

        return AlexaSkillResponse(
            updateDate = dateTime.format(alexaSkillFormatter),
            titleText = messageSource.getMessage("alexa.tomorrow.rating.title", emptyArray(), locale),
            mainText = mainText
        )
    }
}