package ie.daithi.electricityprices.service

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

@Component
@Profile("price-sync")
class PriceManager(
    private val esiosSync: EsiosSync,
    private val reeSync: ReeSync
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun startReeDataSync() {
        logger.info("Starting REE PVPC data sync")

        val executor = Executors.newFixedThreadPool(1) as ThreadPoolExecutor

        executor.submit<Any> { reeSync.start(); null }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun startEsiosDataSync() {

        logger.info("Starting ESIOS PVPC data sync")

        val executor = Executors.newFixedThreadPool(1) as ThreadPoolExecutor

        executor.submit<Any> { esiosSync.start(); null }
    }

}