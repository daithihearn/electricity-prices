package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.AlexaSkillResponse
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.utils.alexaSkillFormatter
import ie.daithi.electricityprices.utils.dateFormatter
import ie.daithi.electricityprices.utils.getCheapestPeriod
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
        responses.add(getDayRating(pricesToday, now, locale))

        // Get next good 3-hour period
        val nextCheapPeriod = getNextCheapPeriod(pricesToday, now, locale)
        nextCheapPeriod?.let { responses.add(it) }


        // Get Tomorrow's price data
        val tomorrow = now.plusDays(1)
        val pricesTomorrow = priceSerice.getPrices(tomorrow.toLocalDate())

        if (pricesTomorrow.isNotEmpty()) {
            // Tomorrow Rating
            responses.add(getDayRating(pricesTomorrow, tomorrow, locale))
        }

        return responses
    }

    private fun getNextCheapPeriod(pricesToday: List<Price>, now: LocalDateTime, locale: Locale): AlexaSkillResponse? {
        val cheapestPeriod = getCheapestPeriod(pricesToday, 3)

        // If the period has passed do nothing
        if (cheapestPeriod[2].dateTime.plusMinutes(59).isBefore(now)) {
            return null
        }

        // Get average price for period
        val averagePrice = cheapestPeriod.map { it.price }.average().times(100).roundToInt()

        // If period hasn't started send message
        if (cheapestPeriod[0].dateTime.isAfter(now)) {
            return AlexaSkillResponse(
                updateDate = now.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.next.cheap.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage("alexa.next.cheap.period.main", arrayOf(cheapestPeriod[0].dateTime.hour, averagePrice), locale)
            )
        } else {
            // We are currently in the good period
            return AlexaSkillResponse(
                updateDate = now.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.current.cheap.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage("alexa.current.cheap.period.main", arrayOf(cheapestPeriod[0].dateTime.hour, averagePrice), locale)
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

    fun getDayRating(prices: List<Price>, dateTime: LocalDateTime, locale: Locale): AlexaSkillResponse {
        val dailyAverage = prices.map { it.price }.average()

        // Get the 30-day average and compare daily average to it. If it's within 2 cents then it's a normal day
        // If it's more than 2 cents then it's a good day
        // If it's less than 2 cents then it's a bad day
        val thirtyDayAverage = priceSerice.getPrices(start = dateTime.toLocalDate().minusDays(30).format(dateFormatter), end = null).map { it.price }.average()

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        // Check if is today or tomorrow
        val day = if (dateTime.toLocalDate() == LocalDateTime.now().toLocalDate()) "today" else "tomorrow"

        val mainText = when {
            dailyAverage > thirtyDayAverage + 2 -> messageSource.getMessage("alexa.$day.rating.main.good", arrayOf(roundedDailyAverage), locale)
            dailyAverage < thirtyDayAverage - 2 -> messageSource.getMessage("alexa.$day.rating.main.bad", arrayOf(roundedDailyAverage), locale)
            else -> messageSource.getMessage("alexa.$day.rating.main.normal", arrayOf(roundedDailyAverage), locale)
        }

        return AlexaSkillResponse(
            updateDate = dateTime.format(alexaSkillFormatter),
            titleText = messageSource.getMessage("alexa.$day.rating.title", emptyArray(), locale),
            mainText = mainText
        )
    }
}