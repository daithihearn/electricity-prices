package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.AlexaSkillResponse
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.utils.alexaSkillFormatter
import ie.daithi.electricityprices.utils.dateFormatter
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

        // Day Rating
        responses.add(getDayRating(pricesToday, now, locale))

        return responses
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

        val mainText = when {
            dailyAverage > thirtyDayAverage + 2 -> messageSource.getMessage("alexa.today.rating.main.good", arrayOf(dailyAverage), locale)
            dailyAverage < thirtyDayAverage - 2 -> messageSource.getMessage("alexa.today.rating.main.bad", arrayOf(dailyAverage), locale)
            else -> messageSource.getMessage("alexa.today.rating.main.normal", arrayOf(dailyAverage), locale)
        }

        return AlexaSkillResponse(
            updateDate = dateTime.format(alexaSkillFormatter),
            titleText = messageSource.getMessage("alexa.day.rating.title", arrayOf(dailyAverage), locale),
            mainText = mainText
        )
    }
}