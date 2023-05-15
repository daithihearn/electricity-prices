package ie.daithi.electricityprices

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@OpenAPIDefinition(
	info = Info(
		title =
		"Electricity Prices API",
		version = "1.1.4",
		description = "Returns PVPC electricity prices for a given range"
	),
	servers = [
		Server(url = "/", description = "Electricity Prices API"),
	]
)
@SpringBootApplication(scanBasePackages = ["ie.daithi.electricityprices"])
class ElectricityPricesApplication

fun main(args: Array<String>) {
	runApplication<ElectricityPricesApplication>(*args)
}
