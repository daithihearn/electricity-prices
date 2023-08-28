package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.DayRating
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

    fun getFullFeed(locale: Locale): String {
        val responses = mutableListOf<String>()

        // Get Today's price data
        val now = LocalDateTime.now()
        val pricesToday = priceSerice.getPrices(now.toLocalDate())

        if (pricesToday.isEmpty()) {
            return responses.joinToString(" ")
        }

        // Today's price and rating
        val thirtyDayAverage = priceSerice.getThirtyDayAverage()
        responses.add(getTodayRating(now, pricesToday, thirtyDayAverage, locale))

        // Get next good 3-hour period
        val nextCheapPeriod = getNextCheapPeriod(now, pricesToday, locale)
        responses.add(nextCheapPeriod)

        // Get next bad 3-hour period
        val nextExpensivePeriod = getNextExpensivePeriod(now, pricesToday, locale)
        responses.add(nextExpensivePeriod)

        // Get Tomorrow's price data
        val tomorrow = now.plusDays(1)
        val tomorrowRating =
            getTomorrowRating(dateTime = tomorrow, thirtyDayAverage = thirtyDayAverage, locale = locale)
        if (tomorrowRating.second) responses.add(tomorrowRating.first)

        return responses.joinToString(" ")
    }

    fun getTodayRating(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceSerice.getThirtyDayAverage(),
        locale: Locale
    ): String {
        val dailyAverage = pricesToday.map { it.price }.average()

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        // Get current price
        val currentPrice = pricesToday.find { it.dateTime.hour == dateTime.hour }

        // Round current price to nearest cent
        val currentPriceCents = currentPrice?.price?.times(100)?.roundToInt()

        val rating = calculateRating(dailyAverage, thirtyDayAverage)

        logger.info("dailyAverage: $dailyAverage thirtyDayAverage: $thirtyDayAverage rating: $rating")

        val mainText = when {
            rating == DayRating.GOOD -> messageSource.getMessage(
                "alexa.today.rating.main.good",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )

            rating == DayRating.BAD -> messageSource.getMessage(
                "alexa.today.rating.main.bad",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )

            else -> messageSource.getMessage(
                "alexa.today.rating.main.normal",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )
        }

        return mainText

    }

    fun getTomorrowRating(
        dateTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        pricesTomorrow: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceSerice.getThirtyDayAverage(),
        locale: Locale
    ): Pair<String, Boolean> {

        if (pricesTomorrow.isEmpty()) {
            return Pair(
                messageSource.getMessage(
                    "alexa.tomorrow.rating.main.no_data", emptyArray(), locale
                ), false
            )
        }

        val dailyAverage = pricesTomorrow.map { it.price }.average()

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        // Get cheapest period
        val cheapestPeriod = getCheapestPeriod(pricesTomorrow, 3)
        val cheapestPeriodAverage = cheapestPeriod.map { it.price }.average().times(100).roundToInt()
        val cheapestPeriodTime = formatAmPm(cheapestPeriod[0].dateTime)

        // Get most expensive period
        val mostExpensivePeriod = getMostExpensivePeriod(pricesTomorrow, 3)
        val mostExpensivePeriodAverage = mostExpensivePeriod.map { it.price }.average().times(100).roundToInt()
        val mostExpensivePeriodTime = formatAmPm(mostExpensivePeriod[0].dateTime)

        val rating = calculateRating(dailyAverage, thirtyDayAverage)

        val mainText = when {
            rating == DayRating.GOOD -> messageSource.getMessage(
                "alexa.tomorrow.rating.main.good",
                arrayOf(
                    roundedDailyAverage,
                    cheapestPeriodTime,
                    cheapestPeriodAverage,
                    mostExpensivePeriodTime,
                    mostExpensivePeriodAverage
                ),
                locale
            )

            rating == DayRating.BAD -> messageSource.getMessage(
                "alexa.tomorrow.rating.main.bad",
                arrayOf(
                    roundedDailyAverage,
                    cheapestPeriodTime,
                    cheapestPeriodAverage,
                    mostExpensivePeriodTime,
                    mostExpensivePeriodAverage
                ),
                locale
            )

            else -> messageSource.getMessage(
                "alexa.tomorrow.rating.main.normal",
                arrayOf(
                    roundedDailyAverage,
                    cheapestPeriodTime,
                    cheapestPeriodAverage,
                    mostExpensivePeriodTime,
                    mostExpensivePeriodAverage
                ),
                locale
            )
        }

        return Pair(
            mainText, true
        )
    }

    fun getNextCheapPeriod(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        locale: Locale
    ): String {
        val twoCheapestPeriods = getTwoCheapestPeriods(pricesToday, 3)

        val nextPeriod =
            if (twoCheapestPeriods.first.isNotEmpty() && !twoCheapestPeriods.first[2].dateTime.plusMinutes(59)
                    .isBefore(dateTime)
            ) {
                twoCheapestPeriods.first
            } else if (twoCheapestPeriods.second.isNotEmpty() && !twoCheapestPeriods.second[2].dateTime.plusMinutes(59)
                    .isBefore(dateTime)
            ) {
                twoCheapestPeriods.second
            } else {
                return messageSource.getMessage(
                    "alexa.next.cheap.period.main.no_data",
                    emptyArray(),
                    locale
                )
            }

        // Get average price for period
        val averagePrice = nextPeriod.map { it.price }.average().times(100).roundToInt()

        val cheapestPeriodTime = formatAmPm(nextPeriod[0].dateTime)

        // If period hasn't started send message
        return if (nextPeriod[0].dateTime.isAfter(dateTime)) {
            messageSource.getMessage(
                "alexa.next.cheap.period.main",
                arrayOf(cheapestPeriodTime, averagePrice, nextPeriod.size),
                locale

            )
        } else {
            // We are currently in the good period
            messageSource.getMessage(
                "alexa.current.cheap.period.main",
                arrayOf(cheapestPeriodTime, averagePrice, nextPeriod.size),
                locale
            )

        }
    }

    fun getNextExpensivePeriod(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        locale: Locale
    ): String {
        val expensivePeriod = getMostExpensivePeriod(pricesToday, 3)

        // If the period has passed do nothing
        if (expensivePeriod[2].dateTime.plusMinutes(59).isBefore(dateTime)) {
            return messageSource.getMessage(
                "alexa.next.expensive.period.main.no_data",
                emptyArray(),
                locale

            )
        }

        // Get average price for period
        val averagePrice = expensivePeriod.map { it.price }.average().times(100).roundToInt()

        val expensivePeriodTime = formatAmPm(expensivePeriod[0].dateTime)

        // If period hasn't started send message
        return if (expensivePeriod[0].dateTime.isAfter(dateTime)) {
            messageSource.getMessage(
                "alexa.next.expensive.period.main",
                arrayOf(expensivePeriodTime, averagePrice),
                locale

            )
        } else {
            // We are currently in the good period
            messageSource.getMessage(
                "alexa.current.expensive.period.main",
                arrayOf(expensivePeriodTime, averagePrice),
                locale

            )
        }
    }

    fun getThirtyDayAverage(
        thirtyDayAverage: Double = priceSerice.getThirtyDayAverage(), locale: Locale
    ): String {
        val thirtyDayAverageRounded = thirtyDayAverage.times(100).roundToInt()
        val thirtyDayAverageText = messageSource.getMessage(
            "alexa.thirty.day.average.main",
            arrayOf(thirtyDayAverageRounded),
            locale
        )

        return thirtyDayAverageText
    }

    fun getDailyAverage(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()), locale: Locale
    ): String {
        if (pricesToday.isEmpty()) {
            return messageSource.getMessage(
                "alexa.today.no_data",
                emptyArray(),
                locale
            )
        }

        val dailyAverage = pricesToday.map { it.price }.average()

        val dailyAverageRounded = dailyAverage.times(100).roundToInt()
        val dailyAverageText = messageSource.getMessage(
            "alexa.daily.average.main",
            arrayOf(dailyAverageRounded),
            locale
        )

        return dailyAverageText
    }

    fun getCurrentPrice(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()), locale: Locale
    ): String {
        val currentPrice = pricesToday.find { it.dateTime.hour == dateTime.hour }
            ?: return messageSource.getMessage(
                "alexa.today.no_data",
                emptyArray(),
                locale
            )

        // Round current price to nearest cent
        val currentPriceCents = currentPrice.price.times(100).roundToInt()

        val currentPriceText = messageSource.getMessage(
            "alexa.current.price.main",
            arrayOf(currentPriceCents),
            locale
        )

        return currentPriceText
    }

}