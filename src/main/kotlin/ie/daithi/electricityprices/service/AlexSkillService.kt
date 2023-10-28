package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.DayRating
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.utils.*
import org.apache.logging.log4j.LogManager
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.roundToInt

@Service
class AlexSkillService(private val priceService: PriceService, private val messageSource: MessageSource) {

    private val logger = LogManager.getLogger(this::class.simpleName)

    fun getFullFeed(locale: Locale): String {
        val responses = mutableListOf<String>()

        // Get Today's price data
        val now = LocalDateTime.now()
        val pricesToday = priceService.getPrices(now.toLocalDate())

        if (pricesToday.isEmpty()) {
            return responses.joinToString(" ")
        }

        // Today's price and rating
        val thirtyDayAverage = priceService.getThirtyDayAverage(now.toLocalDate())
        responses.add(getTodayRating(now, pricesToday, thirtyDayAverage, locale))

        // Get next good 3-hour period
        val nextCheapPeriod = getNextCheapPeriod(now, pricesToday, thirtyDayAverage, locale)
        responses.add(nextCheapPeriod)

        // Get next bad 3-hour period
        val nextExpensivePeriod = getNextExpensivePeriod(now, pricesToday, thirtyDayAverage, locale)
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
        pricesToday: List<Price> = priceService.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceService.getThirtyDayAverage(dateTime.toLocalDate()),
        locale: Locale
    ): String {
        val dailyAverage = calculateAverage(pricesToday)

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        // Get current price
        val currentPrice = pricesToday.find { it.dateTime.hour == dateTime.hour }

        // Round current price to nearest cent
        val currentPriceCents = currentPrice?.price?.times(100)?.roundToInt()

        val rating = calculateRating(dailyAverage, thirtyDayAverage)

        logger.info("dailyAverage: $dailyAverage thirtyDayAverage: $thirtyDayAverage rating: $rating")

        return getTodayRatingText(rating, roundedDailyAverage, currentPriceCents ?: 0, locale)
    }

    fun getTomorrowRating(
        dateTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        pricesTomorrow: List<Price> = priceService.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceService.getThirtyDayAverage(dateTime.toLocalDate()),
        locale: Locale
    ): Pair<String, Boolean> {

        if (pricesTomorrow.isEmpty()) {
            return Pair(
                messageSource.getMessage(
                    "alexa.tomorrow.no_data", emptyArray(), locale
                ), false
            )
        }

        // Get rating text
        val dailyAverage = calculateAverage(pricesTomorrow)
        val roundedDailyAverage = dailyAverage.times(100).roundToInt()
        val rating = calculateRating(dailyAverage, thirtyDayAverage)
        val ratingText = getTomorrowRatingText(rating, roundedDailyAverage, locale)

        // Get the cheapest periods
        val cheapPeriods = getCheapPeriods(pricesTomorrow, thirtyDayAverage)
        val cheapPeriodMessage = getCheapPeriodMessage(cheapPeriods, locale)

        // Get the expensive periods
        val expensePeriods = getExpensivePeriods(pricesTomorrow, thirtyDayAverage)
        val expensivePeriodMessage = getExpensivePeriodMessage(expensePeriods, locale)

        return Pair(
            "$ratingText $cheapPeriodMessage $expensivePeriodMessage", true
        )
    }

    private fun getCheapPeriodMessage(cheapPeriods: List<List<Price>>, locale: Locale): String {

        // The start of the message is different if there is only a single period
        val messageStart = if (cheapPeriods.size == 1) messageSource.getMessage(
            "alexa.tomorrow.periods.cheap.single.start",
            null,
            locale
        ) else
            messageSource.getMessage(
                "alexa.tomorrow.periods.cheap.multiple.start",
                arrayOf(
                    cheapPeriods.size
                ),
                locale
            )

        val messageBody = getPeriodBody(cheapPeriods, locale)

        return "$messageStart $messageBody"
    }

    private fun getExpensivePeriodMessage(expensivePeriods: List<List<Price>>, locale: Locale): String {
        // If more than one period process that message
        val messageStart = if (expensivePeriods.size == 1) messageSource.getMessage(
            "alexa.tomorrow.periods.expensive.single.start",
            null,
            locale
        ) else messageSource.getMessage(
            "alexa.tomorrow.periods.expensive.multiple.start",
            arrayOf(
                expensivePeriods.size
            ),
            locale
        )

        val messageBody = getPeriodBody(expensivePeriods, locale)

        return "$messageStart $messageBody"
    }

    private fun getPeriodBody(periods: List<List<Price>>, locale: Locale): String {
        val messageBody = StringBuilder()
        periods.forEach() { prices ->
            val periodAverage = calculateAverageInCents(prices)
            val periodStart = formatAmPm(prices.first().dateTime)
            val periodEnd = formatAmPm(prices.last().dateTime.plusHours(1))
            messageBody.append(
                messageSource.getMessage(
                    "alexa.tomorrow.periods.item",
                    arrayOf(
                        periodStart,
                        periodEnd,
                        periodAverage,
                    ),
                    locale
                )
            )
        }
        return messageBody.toString()
    }

    private fun getTodayRatingText(
        rating: DayRating,
        roundedDailyAverage: Int,
        currentPriceCents: Int,
        locale: Locale
    ): String {
        return when (rating) {
            DayRating.GOOD -> messageSource.getMessage(
                "alexa.today.rating.good",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )

            DayRating.BAD -> messageSource.getMessage(
                "alexa.today.rating.bad",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )

            else -> messageSource.getMessage(
                "alexa.today.rating.normal",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )
        }
    }

    private fun getTomorrowRatingText(rating: DayRating, roundedDailyAverage: Int, locale: Locale): String {
        return when (rating) {
            DayRating.GOOD -> messageSource.getMessage(
                "alexa.tomorrow.rating.good",
                arrayOf(roundedDailyAverage),
                locale
            )

            DayRating.BAD -> messageSource.getMessage(
                "alexa.tomorrow.rating.bad",
                arrayOf(roundedDailyAverage),
                locale
            )

            else -> messageSource.getMessage(
                "alexa.tomorrow.rating.normal",
                arrayOf(roundedDailyAverage),
                locale
            )
        }
    }

    fun getNextCheapPeriod(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceService.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceService.getThirtyDayAverage(dateTime.toLocalDate()),
        locale: Locale
    ): String {
        val cheapPeriods = getCheapPeriods(pricesToday, thirtyDayAverage)

        val nextPeriod = getNextPeriod(cheapPeriods, dateTime) ?: return messageSource.getMessage(
            "alexa.next.cheap.period.no_data",
            emptyArray(),
            locale
        )

        // Get average price for period
        val averagePrice = calculateAverageInCents(nextPeriod)

        val periodStart = formatAmPm(nextPeriod.first().dateTime)
        val periodEnd = formatAmPm(nextPeriod.last().dateTime.plusHours(1))

        // If period hasn't started send message
        return if (nextPeriod.first().dateTime.isAfter(dateTime)) {
            messageSource.getMessage(
                "alexa.next.cheap.period",
                arrayOf(periodStart, periodEnd, averagePrice),
                locale

            )
        } else {
            // We are currently in the good period
            messageSource.getMessage(
                "alexa.current.cheap.period",
                arrayOf(periodStart, periodEnd, averagePrice),
                locale
            )

        }
    }

    fun getNextExpensivePeriod(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceService.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceService.getThirtyDayAverage(dateTime.toLocalDate()),
        locale: Locale
    ): String {
        val expensivePeriods =
            getExpensivePeriods(pricesToday, thirtyDayAverage)

        if (expensivePeriods.isEmpty()) return messageSource.getMessage(
            "alexa.next.expensive.period.no_data",
            emptyArray(),
            locale
        )

        val nextPeriod = getNextPeriod(expensivePeriods, dateTime) ?: return messageSource.getMessage(
            "alexa.next.expensive.period.none_left",
            emptyArray(),
            locale
        )

        // Get average price for period
        val averagePrice = calculateAverageInCents(expensivePeriods[0])

        val periodStart = formatAmPm(nextPeriod.first().dateTime)
        val periodEnd = formatAmPm(nextPeriod.last().dateTime.plusHours(1))

        // If period hasn't started send message
        return if (nextPeriod.first().dateTime.isAfter(dateTime)) {
            messageSource.getMessage(
                "alexa.next.expensive.period",
                arrayOf(periodStart, periodEnd, averagePrice),
                locale

            )
        } else {
            // We are currently in the good period
            messageSource.getMessage(
                "alexa.current.expensive.period",
                arrayOf(periodStart, periodEnd, averagePrice),
                locale

            )
        }
    }

    private fun getNextPeriod(periods: List<List<Price>>, dateTime: LocalDateTime): List<Price>? {
        periods.forEach() { prices ->
            if (prices.isNotEmpty() && !prices.last().dateTime.plusMinutes(59)
                    .isBefore(dateTime)
            ) {
                return prices
            }
        }
        return null
    }

    fun getThirtyDayAverage(
        thirtyDayAverage: Double = priceService.getThirtyDayAverage(LocalDate.now()), locale: Locale
    ): String {
        val thirtyDayAverageRounded = thirtyDayAverage.times(100).roundToInt()

        return messageSource.getMessage(
            "alexa.thirty.day.average",
            arrayOf(thirtyDayAverageRounded),
            locale
        )
    }

    fun getDailyAverage(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceService.getPrices(dateTime.toLocalDate()), locale: Locale
    ): String {
        if (pricesToday.isEmpty()) {
            return messageSource.getMessage(
                "alexa.today.no_data",
                emptyArray(),
                locale
            )
        }

        val dailyAverage = calculateAverage(pricesToday)

        val dailyAverageRounded = dailyAverage.times(100).roundToInt()

        return messageSource.getMessage(
            "alexa.daily.average",
            arrayOf(dailyAverageRounded),
            locale
        )
    }

    fun getCurrentPrice(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceService.getPrices(dateTime.toLocalDate()), locale: Locale
    ): String {
        val currentPrice = pricesToday.find { it.dateTime.hour == dateTime.hour }
            ?: return messageSource.getMessage(
                "alexa.today.no_data",
                emptyArray(),
                locale
            )

        // Round current price to nearest cent
        val currentPriceCents = currentPrice.price.times(100).roundToInt()

        return messageSource.getMessage(
            "alexa.current.price",
            arrayOf(currentPriceCents),
            locale
        )
    }

}
