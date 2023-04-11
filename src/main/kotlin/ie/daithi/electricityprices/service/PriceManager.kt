package ie.daithi.electricityprices.service

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class PriceManager(private val priceService: PriceService) {
    @Scheduled(cron = "0 */5 0-23 * * *")
    fun updatePriceData() {
        priceService.updatePriceData()
    }
}