package ie.daithi.electricityprices

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ElectricityPricesApplication

fun main(args: Array<String>) {
	runApplication<ElectricityPricesApplication>(*args)
}
