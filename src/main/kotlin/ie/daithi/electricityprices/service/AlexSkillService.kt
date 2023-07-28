package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.alexa.AlexaSkillResponse
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

    fun getFullFeed(locale: Locale): List<AlexaSkillResponse> {
        val responses = mutableListOf<AlexaSkillResponse>()

        // Get Today's price data
        val now = LocalDateTime.now()
        val pricesToday = priceSerice.getPrices(now.toLocalDate())

        if (pricesToday.isEmpty()) {
            return responses
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

        return responses
    }

    fun getTodayRating(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceSerice.getThirtyDayAverage(),
        locale: Locale
    ): AlexaSkillResponse {
        val dailyAverage = pricesToday.map { it.price }.average()

        val roundedDailyAverage = dailyAverage.times(100).roundToInt()

        // Get current price
        val currentPrice = pricesToday.find { it.dateTime.hour == dateTime.hour }

        // Round current price to nearest cent
        val currentPriceCents = currentPrice?.price?.times(100)?.roundToInt()

        val mainText = when {
            dailyAverage > thirtyDayAverage + 2 -> messageSource.getMessage(
                "alexa.today.rating.main.good",
                arrayOf(roundedDailyAverage, currentPriceCents),
                locale
            )

            dailyAverage < thirtyDayAverage - 2 -> messageSource.getMessage(
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

        return AlexaSkillResponse(
            updateDate = dateTime.format(alexaSkillFormatter),
            titleText = messageSource.getMessage("alexa.today.rating.title", emptyArray(), locale),
            mainText = mainText
        )
    }

    fun getTomorrowRating(
        dateTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        pricesTomorrow: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        thirtyDayAverage: Double = priceSerice.getThirtyDayAverage(),
        locale: Locale
    ): Pair<AlexaSkillResponse, Boolean> {

        if (pricesTomorrow.isEmpty()) {
            return Pair(
                AlexaSkillResponse(
                    updateDate = dateTime.format(alexaSkillFormatter),
                    titleText = messageSource.getMessage("alexa.tomorrow.rating.title", emptyArray(), locale),
                    mainText = messageSource.getMessage("alexa.tomorrow.rating.main.no_data", emptyArray(), locale)
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
        val mostExpensivePeriod = getCheapestPeriod(pricesTomorrow, 3)
        val mostExpensivePeriodAverage = mostExpensivePeriod.map { it.price }.average().times(100).roundToInt()
        val mostExpensivePeriodTime = formatAmPm(mostExpensivePeriod[0].dateTime)

        val mainText = when {
            dailyAverage > thirtyDayAverage + 2 -> messageSource.getMessage(
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

            dailyAverage < thirtyDayAverage - 2 -> messageSource.getMessage(
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
            AlexaSkillResponse(
                updateDate = dateTime.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.tomorrow.rating.title", emptyArray(), locale),
                mainText = mainText
            ), true
        )
    }

    fun getNextCheapPeriod(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        locale: Locale
    ): AlexaSkillResponse {
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
                return AlexaSkillResponse(
                    updateDate = dateTime.format(alexaSkillFormatter),
                    titleText = messageSource.getMessage("alexa.next.cheap.period.title", emptyArray(), locale),
                    mainText = messageSource.getMessage(
                        "alexa.next.cheap.period.main.no_data",
                        emptyArray(),
                        locale
                    )
                )

            }

        // Get average price for period
        val averagePrice = nextPeriod.map { it.price }.average().times(100).roundToInt()

        val cheapestPeriodTime = formatAmPm(nextPeriod[0].dateTime)

        // If period hasn't started send message
        return if (nextPeriod[0].dateTime.isAfter(dateTime)) {
            AlexaSkillResponse(
                updateDate = dateTime.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.next.cheap.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage(
                    "alexa.next.cheap.period.main",
                    arrayOf(cheapestPeriodTime, averagePrice),
                    locale
                )
            )
        } else {
            // We are currently in the good period
            AlexaSkillResponse(
                updateDate = dateTime.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.current.cheap.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage(
                    "alexa.current.cheap.period.main",
                    arrayOf(cheapestPeriodTime, averagePrice),
                    locale
                )
            )
        }
    }

    fun getNextExpensivePeriod(
        dateTime: LocalDateTime = LocalDateTime.now(),
        pricesToday: List<Price> = priceSerice.getPrices(dateTime.toLocalDate()),
        locale: Locale
    ): AlexaSkillResponse {
        val expensivePeriod = getMostExpensivePeriod(pricesToday, 3)

        // If the period has passed do nothing
        if (expensivePeriod[2].dateTime.plusMinutes(59).isBefore(dateTime)) {
            return AlexaSkillResponse(
                updateDate = dateTime.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.next.expensive.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage(
                    "alexa.next.expensive.period.main.no_data",
                    emptyArray(),
                    locale
                )
            )
        }

        // Get average price for period
        val averagePrice = expensivePeriod.map { it.price }.average().times(100).roundToInt()

        val expensivePeriodTime = formatAmPm(expensivePeriod[0].dateTime)

        // If period hasn't started send message
        return if (expensivePeriod[0].dateTime.isAfter(dateTime)) {
            AlexaSkillResponse(
                updateDate = dateTime.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.next.expensive.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage(
                    "alexa.next.expensive.period.main",
                    arrayOf(expensivePeriodTime, averagePrice),
                    locale
                )
            )
        } else {
            // We are currently in the good period
            AlexaSkillResponse(
                updateDate = dateTime.format(alexaSkillFormatter),
                titleText = messageSource.getMessage("alexa.current.expensive.period.title", emptyArray(), locale),
                mainText = messageSource.getMessage(
                    "alexa.current.expensive.period.main",
                    arrayOf(expensivePeriodTime, averagePrice),
                    locale
                )
            )
        }
    }


}