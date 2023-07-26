package ie.daithi.electricityprices.web.controller

import com.amazon.ask.servlet.verifiers.AlexaHttpRequest
import com.amazon.ask.servlet.verifiers.SkillRequestSignatureVerifier
import com.amazon.ask.servlet.verifiers.SkillRequestTimestampVerifier
import ie.daithi.electricityprices.exceptions.BadRequestException
import ie.daithi.electricityprices.model.AlexaSkillResponse
import ie.daithi.electricityprices.service.AlexSkillService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.Locale

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alexa", description = "Endpoints that relate to the electricity prices alexa skill")
class AlexaSkillController(private val alexSkillService: AlexSkillService) {

    @GetMapping("/alexa")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get info for the alexa skill",
        description = "Returns the JSON message feed for the electricity prices alexa skill"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getFullFeed(@RequestParam(value = "locale", required = false) locale: String?): List<AlexaSkillResponse> {
        val resolvedLocale = locale?.let { Locale.forLanguageTag(it) } ?: Locale.forLanguageTag("es")
        return alexSkillService.getFullFeed(resolvedLocale)
    }

    @GetMapping("/alexa/today")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get today's price and rating",
        description = "Returns the JSON message for today's price and rating"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getTodayRating(@RequestParam(value = "locale", required = false) locale: String?): AlexaSkillResponse {
        val resolvedLocale = locale?.let { Locale.forLanguageTag(it) } ?: Locale.forLanguageTag("es")
        return alexSkillService.getTodayRating(locale = resolvedLocale)
    }

    @GetMapping("/alexa/tomorrow")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get tomorrow's price and rating",
        description = "Returns the JSON message for tomorrow's price and rating"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getTomorrowRating(@RequestParam(value = "locale", required = false) locale: String?): AlexaSkillResponse {
        val resolvedLocale = locale?.let { Locale.forLanguageTag(it) } ?: Locale.forLanguageTag("es")
        return alexSkillService.getTomorrowRating(locale = resolvedLocale).first
    }

    @GetMapping("/alexa/cheap/next")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get the next cheap period",
        description = "Returns the JSON message for the next cheap period"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getNextCheapPeriod(@RequestParam(value = "locale", required = false) locale: String?): AlexaSkillResponse {
        val resolvedLocale = locale?.let { Locale.forLanguageTag(it) } ?: Locale.forLanguageTag("es")
        return alexSkillService.getNextCheapPeriod(locale = resolvedLocale)
    }

    @GetMapping("/alexa/expensive/next")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Get the next expensive period",
        description = "Returns the JSON message for the next expensive period"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getNextExpensivePeriod(@RequestParam(value = "locale", required = false) locale: String?): AlexaSkillResponse {
        val resolvedLocale = locale?.let { Locale.forLanguageTag(it) } ?: Locale.forLanguageTag("es")
        return alexSkillService.getNextExpensivePeriod(locale = resolvedLocale)
    }

    @PostMapping("/endpoint")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "WIP: Alexa skill endpoint",
        description = "WIP: Endpoint for the alexa skill"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun processAlexaRequest(
        @RequestBody requestBody: String,
        request: AlexaHttpRequest
    ): List<AlexaSkillResponse> {
        // setup verifiers
        val timestampVerifier = SkillRequestTimestampVerifier(10000)
        val signatureVerifier = SkillRequestSignatureVerifier()

        // validate the request
        val isValid: Boolean = try {
            timestampVerifier.verify(request)
            signatureVerifier.verify(request)
            true
        } catch (ex: Exception) {
            false
        }

        // process the request if it's valid, else return an error
        return if (isValid) {
            return alexSkillService.getFullFeed(Locale.forLanguageTag("es"))
        } else {
            // Throw a Bad Request exception
            throw BadRequestException("Invalid request")
        }
    }
}
