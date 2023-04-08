package ie.daithi.electricityprices.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.util.DigestUtils
import java.time.LocalDateTime

@Document(collection = "prices")
@CompoundIndex(name = "date-hour", def = "{'date': 1, 'hour': 1}", unique = true)
data class Price(
    @Id
    val id: String,
    @Indexed
    val dateTime: LocalDateTime,
    val price: Double,
) {
    constructor(dateTime: LocalDateTime, price: Double) : this(
        id = DigestUtils.md5DigestAsHex("${dateTime.year}-${dateTime.monthValue}-${dateTime.dayOfMonth}-${dateTime.hour}".toByteArray()),
        dateTime = dateTime,
        price = price
    )
}