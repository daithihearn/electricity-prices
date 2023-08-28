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
        fun `getCheapestPeriod - cheapest 3 hour period 2023-08-26`() {
            val cheapestPeriod = getCheapestPeriod(prices26, 3)
            assert(cheapestPeriod.size == 3)
            assert(cheapestPeriod[0].id == "07b1debfb24c8d9af3ebad0ba1936cf9")
            assert(cheapestPeriod[1].id == "fc1747d567c846b18e248aa20b195dca")
            assert(cheapestPeriod[2].id == "ce8a40a4ce47b6185d51b5b46937cea3")
        }

        @Test
        fun `getCheapestPeriod - cheapest 3 hour period 2023-08-27`() {
            val cheapestPeriod = getCheapestPeriod(prices27, 3)
            assert(cheapestPeriod.size == 3)
            assert(cheapestPeriod[0].id == "ef04918f3e2c63ae863ea1fe79cee6a0")
            assert(cheapestPeriod[1].id == "68e0b139b3beba16821dfbfe2f3862f1")
            assert(cheapestPeriod[2].id == "01ad85708347f0f78d5a520600284395")
        }
    }

    @Nested
    inner class GetTwoCheapestPeriods {

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-23 - single period`() {
            val period = getTwoCheapestPeriods(prices23, 3)
            assert(period.first.size == 3)
            assert(period.second.isEmpty())

            assert(period.first[0].id == "542e6e3f2adb2656894a232bc4eda185")
            assert(period.first[1].id == "37b77ebd83b92e23c94f1e0a70932ed2")
            assert(period.first[2].id == "ed30ce3ed863368649a9aa4e1b9a0141")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-25 - two periods correct order`() {
            val periods = getTwoCheapestPeriods(prices25, 3)
            assert(periods.first.size == 3)
            assert(periods.second.size == 3)

            assert(periods.first[0].id == "b2956284e7a8360c95c7a96d85c25986")
            assert(periods.first[1].id == "ed22702aa041f315bb627f005e3e4771")
            assert(periods.first[2].id == "9a021ff5cec2249464fcc93facae572b")

            assert(periods.second[0].id == "a5af368ad02371db8101239b38e49c74")
            assert(periods.second[1].id == "1d87e2204fc9a2eb7c273c6a2538a054")
            assert(periods.second[2].id == "9804be2a1bad648f4bdb1ad50240921d")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-18 - two periods incorrect order`() {
            val periods = getTwoCheapestPeriods(prices18, 3)
            assert(periods.first.size == 3)
            assert(periods.second.size == 3)

            assert(periods.first[0].id == "be400151bf0648ec25897a37f4a54160")
            assert(periods.first[1].id == "11d7476fa500d201ccee4147e0ab2145")
            assert(periods.first[2].id == "eb10ebda12ba60af79d9721d3346d53a")

            assert(periods.second[0].id == "7c9c4a8720b93bdc534bed80eb344bcb")
            assert(periods.second[1].id == "a9370544a8990a731f836f62d1eb43fb")
            assert(periods.second[2].id == "5efd1ed569da3a0e96c9e7ba8bede2e6")
        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-26`() {
            val periods = getTwoCheapestPeriods(prices26, 3)
            assert(periods.first.size == 6)
            assert(periods.second.isEmpty())

            assert(periods.first[0].id == "3a9966df749d425622a2601cd7b77a22")
            assert(periods.first[1].id == "ddeef7a2107eb484a1550923ab583dc0")
            assert(periods.first[2].id == "bb2e68a812b52f3d79c03d6060eea7bf")
            assert(periods.first[3].id == "07b1debfb24c8d9af3ebad0ba1936cf9")
            assert(periods.first[4].id == "fc1747d567c846b18e248aa20b195dca")
            assert(periods.first[5].id == "ce8a40a4ce47b6185d51b5b46937cea3")

        }

        @Test
        fun `getTwoCheapestPeriods - cheapest 3 hour period 2023-08-27`() {
            val periods = getTwoCheapestPeriods(prices27, 3)
            assert(periods.first.size == 6)
            assert(periods.second.isEmpty())

            assert(periods.first[0].id == "ef04918f3e2c63ae863ea1fe79cee6a0")
            assert(periods.first[1].id == "68e0b139b3beba16821dfbfe2f3862f1")
            assert(periods.first[2].id == "01ad85708347f0f78d5a520600284395")
            assert(periods.first[3].id == "7e0d5499da953323a3e7224ad7692342")
            assert(periods.first[4].id == "6688009bd148ba9f2c5365870383d787")
            assert(periods.first[5].id == "969f61dec7a9092804bb47b112a496d2")
        }
    }
}