package ie.daithi.electricityprices

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
	info = Info(
		title =
		"Electricity Prices API",
		version = "1.0.0",
		description = "Returns PVPC electricity prices for a given range"
	)
)
@SpringBootApplication(scanBasePackages = ["ie.daithi.electricityprices"])
class ElectricityPricesApplication

fun main(args: Array<String>) {
	runApplication<ElectricityPricesApplication>(*args)
}
