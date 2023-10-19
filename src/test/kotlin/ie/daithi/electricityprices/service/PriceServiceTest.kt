package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.model.*
import ie.daithi.electricityprices.repos.PriceRepo
import ie.daithi.electricityprices.utils.dateFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime

class PriceServiceTest {

    private val priceRepo = mockk<PriceRepo>(relaxed = true)
    private val reeRest = mockk<WebClient>(relaxed = true)
    private val esiosRest = mockk<WebClient>(relaxed = true)

    private val priceService = PriceService(priceRepo, reeRest, esiosRest)

    @Nested
    inner class GetPricesTest {
        @Test
        fun `getPrices - date`() {
            val date = LocalDate.parse("2023-08-25", dateFormatter)
            val mockResponse: List<Price> = listOf(Price(LocalDateTime.now(), 1.0))

            // Mocking the method
            every { priceRepo.dateTimeBetween(any(), any()) } returns mockResponse

            // Invoke the method under test
            val response = priceService.getPrices(date)

            // Verify the call to dateTimeBetween with specific arguments
            verify {
                priceRepo.dateTimeBetween(
                    date.atStartOfDay().minusSeconds(1),
                    date.plusDays(1).atStartOfDay().minusSeconds(1)
                )
            }

            assert(response.size == 1)
            assert(response[0].price == 1.0)
        }

        @Test
        fun `getPrices - start and end`() {
            val start = LocalDateTime.of(2023, 8, 24, 23, 59, 59)
            val end = LocalDateTime.of(2023, 8, 26, 23, 59, 59)
            val mockResponse: List<Price> = listOf(Price(LocalDateTime.now(), 1.0))

            // Mocking the method
            every { priceRepo.dateTimeBetween(any(), any()) } returns mockResponse

            // Invoke the method under test
            val response = priceService.getPrices(start, end)

            // Verify the call to dateTimeBetween with specific arguments
            verify {
                priceRepo.dateTimeBetween(
                    start,
                    end
                )
            }

            assert(response.size == 1)
            assert(response[0].price == 1.0)
        }
    }


}
