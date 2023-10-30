package ie.daithi.electricityprices.web.controller

import ie.daithi.electricityprices.exceptions.DataNotAvailableYetException
import ie.daithi.electricityprices.exceptions.UnprocessableEntityException
import ie.daithi.electricityprices.model.DailyAverage
import ie.daithi.electricityprices.model.DailyPriceInfo
import ie.daithi.electricityprices.model.Price
import ie.daithi.electricityprices.service.PriceService
import ie.daithi.electricityprices.utils.dateFormatter
import ie.daithi.electricityprices.web.validaton.ValidDateDay
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Price", description = "Endpoints that relate to electricity prices")
class PriceController(
    private val priceSerice: PriceService
) {

    @GetMapping("/price")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get price info",
        description = "Returns price info for the date provided. If no date is provided it " +
                "defaults to today. The day should be given in a string form yyyy-MM-dd"
    )
    @Parameter(
        `in` = ParameterIn.QUERY,
        name = "date",
        schema = Schema(type = "string", pattern = "\\d{4}-\\d{2}-\\d{2}"),
        description = "The date to query (defaults to today)",
        required = false,
        example = "2023-08-30"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(
                responseCode = "422", description = "Invalid date", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UnprocessableEntityException::class),
                    examples = [
                        ExampleObject(
                            value = "{\n" +
                                    "  \"timestamp\": \"2023-09-10T10:03:38.111+00:00\",\n" +
                                    "  \"status\": 422,\n" +
                                    "  \"error\": \"Unprocessable Entity\",\n" +
                                    "  \"message\": \"\"getPrices.start: The provided date is invalid. It must match yyyy-MM-dd\",\n" +
                                    "  \"path\": \"/api/v1/price?start=junk\"\n" +
                                    "}"
                        )
                    ]
                )]
            )
        ]
    )
    @ResponseBody
    fun getPrices(
        @ValidDateDay @RequestParam(required = false) date: String?,
    ): List<Price> {
        date ?: return priceSerice.getTodayPrices()
        return priceSerice.getPrices(LocalDate.parse(date, dateFormatter))
    }

    @GetMapping("/price/dailyinfo")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get price info", description = "Returns price info for the date provided. " +
                "If no date is provided the default is the current day. Dates should be given in a string form yyyy-MM-dd"
    )
    @Parameter(
        `in` = ParameterIn.QUERY,
        name = "date",
        schema = Schema(type = "string", pattern = "\\d{4}-\\d{2}-\\d{2}"),
        description = "Address of the transaction origin",
        required = false,
        example = "2023-08-30"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(
                responseCode = "404", description = "Data not available yet", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = DataNotAvailableYetException::class),
                    examples = [
                        ExampleObject(
                            value = "{\n" +
                                    "  \"timestamp\": \"2023-09-10T10:03:38.111+00:00\",\n" +
                                    "  \"status\": 404,\n" +
                                    "  \"error\": \"Not Found\",\n" +
                                    "  \"message\": \"No data available for 2024-01-01\",\n" +
                                    "  \"path\": \"/api/v1/price/dailyinfo?date=2024-01-01\"\n" +
                                    "}"
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "422", description = "Invalid date", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UnprocessableEntityException::class),
                    examples = [
                        ExampleObject(
                            value = "{\n" +
                                    "  \"timestamp\": \"2023-09-10T10:03:38.111+00:00\",\n" +
                                    "  \"status\": 422,\n" +
                                    "  \"error\": \"Unprocessable Entity\",\n" +
                                    "  \"message\": \"\"getDailyPriceInfo.date: The provided date is invalid. It must match yyyy-MM-dd\",\n" +
                                    "  \"path\": \"/api/v1/price/dailyinfo/incorrect\"\n" +
                                    "}"
                        )
                    ]
                )]
            )
        ]
    )
    @ResponseBody
    fun getDailyPriceInfo(@ValidDateDay @RequestParam(required = false) date: String?): DailyPriceInfo {
        date ?: return priceSerice.getTodayPriceInfo()
        return priceSerice.getDailyPriceInfo(LocalDate.parse(date, dateFormatter))
    }

    @GetMapping("/price/averages")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get 30 day averages",
        description = "Return average values for the 30 days before the date provided. " +
                "Dates should be given in a string form yyyy-MM-dd"
    )
    @Parameter(
        `in` = ParameterIn.QUERY,
        name = "date",
        schema = Schema(type = "string", pattern = "\\d{4}-\\d{2}-\\d{2}"),
        description = "Address of the transaction origin",
        required = false,
        example = "2023-08-30"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Request successful"),
            ApiResponse(
                responseCode = "404", description = "Data not available", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = DataNotAvailableYetException::class),
                    examples = [
                        ExampleObject(
                            value = "{\n" +
                                    "  \"timestamp\": \"2023-09-10T10:03:38.111+00:00\",\n" +
                                    "  \"status\": 404,\n" +
                                    "  \"error\": \"Not Found\",\n" +
                                    "  \"message\": \"No data available for 2024-01-01\",\n" +
                                    "  \"path\": \"/api/v1/price/averages?date=2024-01-01\"\n" +
                                    "}"
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "422", description = "Invalid date", content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UnprocessableEntityException::class),
                    examples = [
                        ExampleObject(
                            value = "{\n" +
                                    "  \"timestamp\": \"2023-09-10T10:03:38.111+00:00\",\n" +
                                    "  \"status\": 422,\n" +
                                    "  \"error\": \"Unprocessable Entity\",\n" +
                                    "  \"message\": \"\"getDailyPriceInfo.date: The provided date is invalid. It must match yyyy-MM-dd\",\n" +
                                    "  \"path\": \"/api/v1/price/averages/incorrect\"\n" +
                                    "}"
                        )
                    ]
                )]
            )
        ]
    )
    @ResponseBody
    fun getThirtyDayAverages(@ValidDateDay @RequestParam(required = false) date: String?): List<DailyAverage> {
        val day = date?.let { LocalDate.parse(it, dateFormatter) } ?: LocalDate.now()
        return priceSerice.getDailyAverages(day, 30)
    }
}
