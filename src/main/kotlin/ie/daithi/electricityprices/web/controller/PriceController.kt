package ie.daithi.electricityprices.web.controller

import ie.daithi.electricityprices.model.DailyPriceInfo
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.service.PriceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Price", description = "Endpoints that relate to electricity prices")
class PriceController(
    private val priceSerice: PriceService
) {

    @GetMapping("/price")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get price info", description = "Returns price info within the range provided. " +
                "If no start date is provided the default is the start of the current day. If no end date is " +
                "provided the default is the end of today. Dates should be given in a string form yyyy-MM-dd"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getPrices(
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?
    ): List<Price> {
        return priceSerice.getPrices(start, end)
    }

    @GetMapping("/price/dailyinfo/{date}")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get price info", description = "Returns price info for the date provided. " +
                "If no date is provided the default is the current day. Dates should be given in a string form yyyy-MM-dd"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getDailyPriceInfo(@PathVariable date: String?): DailyPriceInfo {
        return priceSerice.getDailyPriceInfo(date)
    }
}