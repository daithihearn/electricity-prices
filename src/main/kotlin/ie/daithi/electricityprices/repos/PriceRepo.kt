package ie.daithi.electricityprices.repos

import ie.daithi.electricityprices.model.Price
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PriceRepo : CrudRepository<Price, String> {
    fun dateTimeBetween(endDate: LocalDateTime, startDate: LocalDateTime): List<Price>
}