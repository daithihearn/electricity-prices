package ie.daithi.electricityprices.web.controller

import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.service.PriceService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1")
@Api(tags = ["Price"], description = "Endpoints that relate to electricity prices")
class PriceController(
        private val priceSerice: PriceService
) {

    @GetMapping("/price")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiOperation(value = "Get price info", notes = "Returns price info within the range provided. If no start date is provided the default is the start of the current day. If no end date is provided the default is the end of today. Dates should be given in a string form yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @ApiResponses(
            ApiResponse(code = 200, message = "Request successful")
    )
    @ResponseBody
    fun getPrices(@RequestParam(required = false) start: String?, @RequestParam(required = false) end: String?): List<Price> {
        return priceSerice.getPrices(end, start)
    }
}