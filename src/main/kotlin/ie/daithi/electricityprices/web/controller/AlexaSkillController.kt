package ie.daithi.electricityprices.web.controller

import ie.daithi.electricityprices.model.AlexaSkillResponse
import ie.daithi.electricityprices.service.PriceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alexa", description = "Endpoints that relate to the electricity prices alexa skill")
class AlexaSkillController(private val priceSerice: PriceService) {
    @GetMapping("/alexa")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get info for the alexa skill", description = "Returns the JSON message feed for the electricity prices alexa skill")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getFeed (): AlexaSkillResponse {
        val now = LocalDateTime.now()
        val prices = priceSerice.getPrices(now.toLocalDate())

        // Get current price
        val currentPrice = prices.find { it.dateTime.hour == now.hour }

        return AlexaSkillResponse(
            updateDate = now.toString(),
            titleText = "Electricity Prices",
            mainText = "The current price is ${currentPrice?.price} cents per kWh"
        )
    }
}