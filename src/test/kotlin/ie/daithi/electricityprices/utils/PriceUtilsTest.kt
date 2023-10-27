package ie.daithi.electricityprices.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ie.daithi.electricityprices.model.DayRating
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

    private val prices17 =
        objectMapper.readValue(File("src/test/resources/prices-2023-10-17.json"), typeRef)

    private val pricesVeryCheap =  objectMapper.readValue(File("src/test/resources/prices-very-cheap.json"), typeRef)

    @Nested
    inner class CalculateCheapVariance {
        @Test
        fun `calculateCheapVariance - empty list`() {
            val variance = calculateCheapVariance(emptyList(), 0.0)
            assert(variance == 0.0)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-18 - normal daily average`() {
            val variance = calculateCheapVariance(prices18, 0.15)
            assert(variance == 0.016479999999999998)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-23 - normal daily average`() {
            val variance = calculateCheapVariance(prices23, 0.15)
            assert(variance == 0.025083333333333336)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-25 - normal daily average`() {
            val variance = calculateCheapVariance(prices25, 0.15)
            assert(variance == 0.020246666666666673)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-26 - normal daily average`() {
            val variance = calculateCheapVariance(prices26, 0.15)
            assert(variance == 0.013900972222222217)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-27 - normal daily average`() {
            val variance = calculateCheapVariance(prices27, 0.15)
            assert(variance == 0.041066944444444455)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-27 - low daily average`() {
            val variance = calculateCheapVariance(prices27, 0.0)
            assert(variance == 0.023690000000000003)
        }

        @Test
        fun `calculateCheapVariance - 2023-08-27 - high daily average`() {
            val variance = calculateCheapVariance(prices27, 0.50)
            assert(variance == 0.047380000000000005)
        }
    }

    @Nested
    inner class CalculateExpensiveVariance {
        @Test
        fun `calculateExpensiveVariance - empty list`() {
            val variance = calculateExpensiveVariance(emptyList(), 0.0)
            assert(variance == 0.0)
        }

        @Test
        fun `calculateExpensiveVariance - 2023-08-18 - normal daily average`() {
            val variance = calculateExpensiveVariance(prices18, 0.15)
            assert(variance == 0.032959999999999996)
        }

        @Test
        fun `calculateExpensiveVariance - 2023-08-18 - high daily average`() {
            val variance = calculateExpensiveVariance(prices18, 0.50)
            assert(variance == 0.016479999999999998)
        }

        @Test
        fun `calculateExpensiveVariance - 2023-08-18 - low daily average`() {
            val variance = calculateExpensiveVariance(prices18, 0.0)
            assert(variance == 0.032959999999999996)
        }

        @Test
        fun `calculateExpensiveVariance - 2023-08-23 - normal daily average`() {
            val variance = calculateExpensiveVariance(prices23, 0.15)
            assert(variance == 0.05016666666666667)
        }
    }

    @Nested
    inner class IsWithinCheapPriceVariance {

        @Test
        fun `isWithinCheapPriceVariance - price is within variance of cheap price`() {
            val isWithinVariance = isWithinCheapPriceVariance(0.12, 0.11, 0.02)
            assert(isWithinVariance)
        }

        @Test
        fun `isWithinCheapPriceVariance - price is not within variance`() {
            val isWithinVariance = isWithinCheapPriceVariance(0.12, 0.08, 0.02)
            assert(!isWithinVariance)
        }

        @Test
        fun `isWithinCheapPriceVariance - cheap price is higher than price`() {
            val isWithinVariance = isWithinCheapPriceVariance(0.12, 0.20, 0.02)
            assert(isWithinVariance)
        }
    }

    @Nested
    inner class GetCheapPeriods {

        @Test
        fun `getCheapPeriods - empty list`() {
            val periods = getCheapPeriods(emptyList(), 0.15)
            assert(periods.isEmpty())
        }

        @Test
        fun `getCheapPeriods - 2023-08-18`() {
            val periods = getCheapPeriods(prices18, 0.15)

            assert(periods.size == 2)
            assert(periods[0].size == 6)
            assert(periods[0][0].id == "02:00")
            assert(periods[0][1].id == "03:00")
            assert(periods[0][2].id == "04:00")
            assert(periods[0][3].id == "05:00")
            assert(periods[0][4].id == "06:00")
            assert(periods[0][5].id == "07:00")

            assert(periods[1].size == 4)
            assert(periods[1][0].id == "14:00")
            assert(periods[1][1].id == "15:00")
            assert(periods[1][2].id == "16:00")
            assert(periods[1][3].id == "17:00")
        }

        @Test
        fun `getCheapPeriods - 2023-08-23`() {
            val periods = getCheapPeriods(prices23, 0.15)

            assert(periods.size == 1)
            assert(periods[0].size == 4)
            assert(periods[0][0].id == "04:00")
            assert(periods[0][1].id == "05:00")
            assert(periods[0][2].id == "06:00")
            assert(periods[0][3].id == "07:00")
        }

        @Test
        fun `getCheapPeriods - 2023-08-25`() {
            val periods = getCheapPeriods(prices25, 0.15)

            assert(periods.size == 2)
            assert(periods[0].size == 6)
            assert(periods[0][0].id == "00:00")
            assert(periods[0][1].id == "01:00")
            assert(periods[0][2].id == "02:00")
            assert(periods[0][3].id == "03:00")
            assert(periods[0][4].id == "04:00")
            assert(periods[0][5].id == "05:00")

            assert(periods[1].size == 3)
            assert(periods[1][0].id == "15:00")
            assert(periods[1][1].id == "16:00")
            assert(periods[1][2].id == "17:00")
        }

        @Test
        fun `getCheapPeriods - 2023-08-26`() {
            val periods = getCheapPeriods(prices26, 0.15)

            assert(periods.size == 1)

            assert(periods[0].size == 8)
            assert(periods[0][0].id == "11:00")
            assert(periods[0][1].id == "12:00")
            assert(periods[0][2].id == "13:00")
            assert(periods[0][3].id == "14:00")
            assert(periods[0][4].id == "15:00")
            assert(periods[0][5].id == "16:00")
            assert(periods[0][6].id == "17:00")
            assert(periods[0][7].id == "18:00")

        }

        @Test
        fun `getCheapPeriods - 2023-08-27`() {
            val periods = getCheapPeriods(prices27, 0.15)

            assert(periods.size == 1)
            assert(periods[0].size == 10)
            assert(periods[0][0].id == "09:00")
            assert(periods[0][1].id == "10:00")
            assert(periods[0][2].id == "11:00")
            assert(periods[0][3].id == "12:00")
            assert(periods[0][4].id == "13:00")
            assert(periods[0][6].id == "15:00")
            assert(periods[0][7].id == "16:00")
            assert(periods[0][8].id == "17:00")
            assert(periods[0][9].id == "18:00")

        }

        @Test
        fun `getCheapPeriods - 2023-10-17`() {
            val periods = getCheapPeriods(prices17, 0.15)

            assert(periods.size == 3)
            assert(periods[0].size == 6)
            assert(periods[0][0].id == "01:00")
            assert(periods[0][1].id == "02:00")
            assert(periods[0][2].id == "03:00")
            assert(periods[0][3].id == "04:00")
            assert(periods[0][4].id == "05:00")
            assert(periods[0][5].id == "06:00")

            assert(periods[1].size == 4)
            assert(periods[1][0].id == "14:00")
            assert(periods[1][1].id == "15:00")
            assert(periods[1][2].id == "16:00")
            assert(periods[1][3].id == "17:00")

            assert(periods[2].size == 1)
            assert(periods[2][0].id == "23:00")

        }

    }

    @Nested
    inner class GetExpensivePeriods {
        @Test
        fun `getExpensivePeriods - empty list`() {
            val periods = getExpensivePeriods(emptyList(), 0.15)
            assert(periods.isEmpty())
        }

        @Test
        fun `getExpensivePeriods - 2023-08-18`() {
            val periods = getExpensivePeriods(prices18, 0.15)
            assert(periods.size == 1)
            assert(periods[0].size == 3)
            assert(periods[0][0].id == "19:00")
            assert(periods[0][1].id == "20:00")
            assert(periods[0][2].id == "21:00")
        }

        @Test
        fun `getExpensivePeriods - 2023-08-23`() {
            val periods = getExpensivePeriods(prices23, 0.15)
            assert(periods.size == 1)
            assert(periods[0].size == 4)
            assert(periods[0][0].id == "20:00")
            assert(periods[0][1].id == "21:00")
            assert(periods[0][2].id == "22:00")
            assert(periods[0][3].id == "23:00")
        }

        @Test
        fun `getExpensivePeriods - 2023-08-25`() {
            val periods = getExpensivePeriods(prices25, 0.15)
            assert(periods.size == 2)
            assert(periods[0].size == 2)
            assert(periods[0][0].id == "10:00")
            assert(periods[0][1].id == "11:00")

            assert(periods[1].size == 3)
            assert(periods[1][0].id == "19:00")
            assert(periods[1][1].id == "20:00")
            assert(periods[1][2].id == "21:00")
        }

        @Test
        fun `getExpensivePeriods - 2023-08-26`() {
            val periods = getExpensivePeriods(prices26, 0.15)
            assert(periods.size == 1)
            assert(periods[0].size == 3)
            assert(periods[0][0].id == "20:00")
            assert(periods[0][1].id == "21:00")
            assert(periods[0][2].id == "22:00")
        }

        @Test
        fun `getExpensivePeriods - 2023-08-27`() {
            val periods = getExpensivePeriods(prices27, 0.15)
            assert(periods.size == 2)
            assert(periods[0].size == 1)
            assert(periods[0][0].id == "00:00")

            assert(periods[1].size == 4)
            assert(periods[1][0].id == "20:00")
            assert(periods[1][1].id == "21:00")
            assert(periods[1][2].id == "22:00")
            assert(periods[1][3].id == "23:00")
        }

        @Test
        fun `getExpensivePeriods - 2023-10-17`() {
            val periods = getExpensivePeriods(prices17, 0.15)
            assert(periods.size == 3)
            assert(periods[0].size == 1)
            assert(periods[0][0].id == "08:00")

            assert(periods[1].size == 2)
            assert(periods[1][0].id == "10:00")
            assert(periods[1][1].id == "11:00")

            assert(periods[2].size == 4)
            assert(periods[2][0].id == "18:00")
            assert(periods[2][1].id == "19:00")
            assert(periods[2][2].id == "20:00")
            assert(periods[2][3].id == "21:00")
        }
    }

    @Nested
    inner class CalculateRating {
        @Test
        fun `calculateRating - very cheap`() {
            val rating = calculateRating(calculateAverage(pricesVeryCheap), 0.17)
            assert(rating == DayRating.GOOD)
        }
    }
}
