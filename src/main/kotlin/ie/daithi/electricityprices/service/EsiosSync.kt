package ie.daithi.electricityprices.service

import ie.daithi.electricityprices.exceptions.DataNotAvailableYetException
import ie.daithi.electricityprices.utils.dateFormatter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
open class EsiosSync(private val priceService: PriceService) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private var lastSyncedDate: LocalDate = LocalDate.parse("2014-03-31", dateFormatter)

    fun start() {
        logger.info("Starting ESIOS PVPC sync from: $lastSyncedDate")
        run()
    }

    private tailrec fun run() {
        val nextDate = lastSyncedDate.plusDays(1)
        try {
            logger.info("Syncing PVPC data for $nextDate")
            priceService.syncEsiosData(nextDate)
            lastSyncedDate = nextDate
        } catch (e: DataNotAvailableYetException) {
            logger.info("Data not available yet for $nextDate. Backing off for 15 minutes")
            Thread.sleep(15 * 60 * 1000)
        } catch (e: Exception) {
            logger.error("Failed to sync PVPC data for $nextDate. Backing off for 2 minutes", e)
            Thread.sleep(2 * 60 * 1000)
        }
        run()
    }
}