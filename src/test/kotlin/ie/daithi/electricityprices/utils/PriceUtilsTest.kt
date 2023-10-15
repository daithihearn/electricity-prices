package ie.daithi.electricityprices.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ie.daithi.electricityprices.model.Price
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class PriceUtilsTest {

    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val typeRef: TypeReference<List<Price>> = object : TypeReference<List<Price>>() {}

    private val prices18 = objectMapper.readValue(File("src/test/resources/prices-2023-08-18.json"), typeRef)

    private val prices23 = objectMapper.readValue(File("src/test/resources/prices-2023-08-23.json"), typeRef)

    private val prices25 =
        objectMapper.readValue(File("src/test/resources/prices-2023-08-25.json"), typeRef)

    private val prices26 =
        objectMapper.readValue(File("src/test/resources/prices-2023-08-26.json"), typeRef)

    private val prices27 =
        objectMapper.readValue(File("src/test/resources/prices-2023-08-27.json"), typeRef)

    @Nested
    inner class GetCheapestPeriod {

        @Test
        fun `getCheapestPeriod - variable period length - empty list`() {
            val cheapestPeriod = getCheapestPeriod(emptyList())
            assert(cheapestPeriod.isEmpty())
        }

        @Test
        fun `getCheapestPeriod - variable period length 2023-08-18`() {
            val cheapestPeriod = getCheapestPeriod(prices18)
            assert(cheapestPeriod.size == 4)
            assert(cheapestPeriod[0].id == "14:00")
            assert(cheapestPeriod[1].id == "15:00")
            assert(cheapestPeriod[2].id == "16:00")
            assert(cheapestPeriod[3].id == "17:00")
        }

        @Test
        fun `getCheapestPeriod - variable period length 2023-08-25`() {
            val cheapestPeriod = getCheapestPeriod(prices25)
            assert(cheapestPeriod.size == 6)
            assert(cheapestPeriod[0].id == "00:00")
            assert(cheapestPeriod[1].id == "01:00")
            assert(cheapestPeriod[2].id == "02:00")
            assert(cheapestPeriod[3].id == "03:00")
            assert(cheapestPeriod[4].id == "04:00")
            assert(cheapestPeriod[5].id == "05:00")
        }

        @Test
        fun `getCheapestPeriod - cheapest 3 hour period 2023-08-26`() {
            val cheapestPeriod = getCheapestPeriod(prices26, 3)
            assert(cheapestPeriod.size == 3)
            assert(cheapestPeriod[0].id == "14:00")
            assert(cheapestPeriod[1].id == "15:00")
            assert(cheapestPeriod[2].id == "16:00")
        }

        @Test
        fun `getCheapestPeriod - cheapest 3 hour period 2023-08-27`() {
            val cheapestPeriod = getCheapestPeriod(prices27, 3)
            assert(cheapestPeriod.size == 3)
            assert(cheapestPeriod[0].id == "12:00")
            assert(cheapestPeriod[1].id == "13:00")
            assert(cheapestPeriod[2].id == "14:00")
        }
    }

    @Nested
    inner class GetTwoCheapestPeriods {

        @Test
        fun `getTwoCheapestPeriods - variable period length`() {
            val periods = getTwoCheapestPeriods(emptyList())
            assert(periods.first.isEmpty())
            assert(periods.second.isEmpty())
        }

        @Test
        fun `getTwoCheapestPeriods - variable period length 2023-08-18`() {
            val periods = getTwoCheapestPeriods(prices18)

            assert(periods.first.size == 8)
            assert(periods.first[0].id == "00:00")
            assert(periods.first[1].id == "01:00")
            assert(periods.first[2].id == "02:00")
            assert(periods.first[3].id == "03:00")
            assert(periods.first[4].id == "04:00")
            assert(periods.first[5].id == "05:00")
            assert(periods.first[6].id == "06:00")
            assert(periods.first[7].id == "07:00")

            assert(periods.second.size == 4)
            assert(periods.second[0].id == "14:00")
            assert(periods.second[1].id == "15:00")
            assert(periods.second[2].id == "16:00")
            assert(periods.second[3].id == "17:00")
        }

        @Test
        fun `getTwoCheapestPeriods - variable period length 2023-08-23`() {
            val periods = getTwoCheapestPeriods(prices23)

            assert(periods.first.size == 4)
            assert(periods.first[0].id == "04:00")
            assert(periods.first[1].id == "05:00")
            assert(periods.first[2].id == "06:00")
            assert(periods.first[3].id == "07:00")

            assert(periods.second.isEmpty())
        }

        @Test
        fun `getTwoCheapestPeriods - variable period length 2023-08-25`() {
            val periods = getTwoCheapestPeriods(prices25)

            assert(periods.first.size == 6)
            assert(periods.first[0].id == "00:00")
            assert(periods.first[1].id == "01:00")
            assert(periods.first[2].id == "02:00")
            assert(periods.first[3].id == "03:00")
            assert(periods.first[4].id == "04:00")
            assert(periods.first[5].id == "05:00")

            assert(periods.second.size == 4)
            assert(periods.second[0].id == "14:00")
            assert(periods.second[1].id == "15:00")
            assert(periods.second[2].id == "16:00")
            assert(periods.second[3].id == "17:00")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-23 - single period`() {
            val period = getTwoCheapestPeriods(prices23, 3)
            assert(period.first.size == 3)
            assert(period.second.isEmpty())

            assert(period.first[0].id == "05:00")
            assert(period.first[1].id == "06:00")
            assert(period.first[2].id == "07:00")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-25 - two periods correct order`() {
            val periods = getTwoCheapestPeriods(prices25, 3)
            assert(periods.first.size == 3)
            assert(periods.second.size == 3)

            assert(periods.first[0].id == "02:00")
            assert(periods.first[1].id == "03:00")
            assert(periods.first[2].id == "04:00")

            assert(periods.second[0].id == "15:00")
            assert(periods.second[1].id == "16:00")
            assert(periods.second[2].id == "17:00")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-18 - two periods incorrect order`() {
            val periods = getTwoCheapestPeriods(prices18, 3)
            assert(periods.first.size == 3)
            assert(periods.second.size == 3)

            assert(periods.first[0].id == "03:00")
            assert(periods.first[1].id == "04:00")
            assert(periods.first[2].id == "05:00")

            assert(periods.second[0].id == "14:00")
            assert(periods.second[1].id == "15:00")
            assert(periods.second[2].id == "16:00")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-26`() {
            val periods = getTwoCheapestPeriods(prices26, 3)
            assert(periods.first.size == 6)
            assert(periods.second.isEmpty())

            assert(periods.first[0].id == "11:00")
            assert(periods.first[1].id == "12:00")
            assert(periods.first[2].id == "13:00")
            assert(periods.first[3].id == "14:00")
            assert(periods.first[4].id == "15:00")
            assert(periods.first[5].id == "16:00")

        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-27`() {
            val periods = getTwoCheapestPeriods(prices27, 3)
            assert(periods.first.size == 6)
            assert(periods.second.isEmpty())

            assert(periods.first[0].id == "12:00")
            assert(periods.first[1].id == "13:00")
            assert(periods.first[2].id == "14:00")
            assert(periods.first[3].id == "15:00")
            assert(periods.first[4].id == "16:00")
            assert(periods.first[5].id == "17:00")
        }
    }

    @Nested
    inner class GetMostExpensivePeriod {

        @Test
        fun `getMostExpensivePeriod - variable period length - empty list`() {
            val cheapestPeriod = getMostExpensivePeriod(emptyList())
            assert(cheapestPeriod.isEmpty())
        }

        @Test
        fun `getMostExpensivePeriod - variable period length 2023-08-27`() {
            val period = getMostExpensivePeriod(prices27)
            assert(period.size == 2)

            assert(period[0].id == "21:00")
            assert(period[1].id == "22:00")
        }

        @Test
        fun `getMostExpensivePeriod - 2023-08-27`() {
            val period = getMostExpensivePeriod(prices27, 3)
            assert(period.size == 3)

            assert(period[0].id == "20:00")
            assert(period[1].id == "21:00")
            assert(period[2].id == "22:00")
        }
    }
}
