package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.exceptions.DataNotAvailableYetException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
open class ReeSync(private val priceService: PriceService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private var lastSyncedDate: LocalDate = LocalDate.now()

    fun start() {
        logger.info("Starting REE PVPC sync")
        run()
    }

    private tailrec fun run() {
        val nextDate = lastSyncedDate.plusDays(1)
        try {
            logger.info("Syncing PVPC data for $nextDate")
            priceService.updatePriceData(nextDate)
            lastSyncedDate = nextDate
            // Wait until the next day at 20:00
            val now = LocalDateTime.now()
            val tomorrowAtEight = LocalDate.now().plusDays(1).atTime(20, 0)
            val backoff = now.until(tomorrowAtEight, ChronoUnit.MILLIS) + 2000
            logger.info("Data synced for $nextDate. Backing off for $backoff millis until 20:00 tomorrow")
            Thread.sleep(backoff)
        } catch (e: DataNotAvailableYetException) {

            // If it's before 20:00, back off until 20:00

            val now = LocalDateTime.now()
            val todayAtEight = LocalDate.now().atTime(20, 0)

            if (now.isBefore(todayAtEight)) {
                val backoff = now.until(todayAtEight, ChronoUnit.MILLIS) + 2000
                logger.info("Data not available yet for $nextDate. Backing off until for $backoff millis until 20:00")
                Thread.sleep(now.until(todayAtEight, ChronoUnit.MILLIS))
            } else {
                logger.info("Data not available yet for $nextDate. Backing off for 1 minute")
                Thread.sleep(1 * 60 * 1000)
            }
        } catch (e: Exception) {
            logger.error("Failed to sync PVPC data for $nextDate. Backing off for 2 minutes", e)
            Thread.sleep(2 * 60 * 1000)
        }
        run()
    }
}