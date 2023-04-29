package ie.daithi.electricityprices.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

@Component
@EnableScheduling
class PriceManager(private val priceService: PriceService, private val pvpcSync: PVPCSync) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun startPVPCCrawler() {

        logger.info("Starting PVPC sync")

        val executor = Executors.newFixedThreadPool(1) as ThreadPoolExecutor

        executor.submit<Any> { pvpcSync.start(); null }
    }

}