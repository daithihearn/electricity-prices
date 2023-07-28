package ie.daithi.electricityprices.web.controller

import com.fasterxml.jackson.databind.ObjectMapper
import ie.daithi.electricityprices.model.alexa.*
import ie.daithi.electricityprices.model.alexa.enums.Intent
import ie.daithi.electricityprices.service.AlexSkillService
import ie.daithi.electricityprices.web.security.AlexaValidationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.Locale

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alexa", description = "Endpoints that relate to the electricity prices alexa skill")
class AlexaSkillController(
    private val alexSkillService: AlexSkillService,
    private val validationService: AlexaValidationService,
    private val mapper: ObjectMapper
) {

    private val logger = LogManager.getLogger(this::class.simpleName)

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

    @PostMapping("/alexa-skill")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(
        summary = "Alexa skill endpoint",
        description = "Endpoint for the alexa skill"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun processAlexaRequest(@RequestBody rawBody: String, request: HttpServletRequest): AlexaResponse {
        // Map the rawBody to an AlexaRequest object using the jackson mapper
        val body = mapper.readValue(rawBody, AlexaRequest::class.java)

        validationService.validate(request, rawBody, body)

        val locale = Locale.forLanguageTag("en")

        val intent = body.request?.intent?.name

        val response = when (intent) {
            Intent.CANCEL.value -> Pair("Goodbye!", true)
            Intent.HELP.value -> Pair("Say, tell me the current prices", false)
            Intent.STOP.value -> Pair("Goodbye!", true)
            Intent.NAVIGATE_HOME.value -> Pair("", false)
            Intent.FALLBACK.value -> Pair(
                "Welcome to the electricity prices skill. Say, tell me the current prices",
                false
            )

            Intent.FULL.value -> Pair(alexSkillService.getFullFeed(locale).map { it.mainText }.joinToString(" "), false)
            Intent.TODAY.value -> Pair(alexSkillService.getTodayRating(locale = locale).mainText, false)
            Intent.TOMORROW.value -> Pair(alexSkillService.getTomorrowRating(locale = locale).first.mainText, false)
            Intent.NEXT_CHEAP.value -> Pair(alexSkillService.getNextCheapPeriod(locale = locale).mainText, false)
            Intent.NEXT_EXPENSIVE.value -> Pair(
                alexSkillService.getNextExpensivePeriod(locale = locale).mainText,
                false
            )

            else -> Pair(
                "Welcome to the electricity prices skill. Say, tell me the current prices",
                false
            )
        }
        val outputSpeech = OutputSpeech(text = response.first)
        val responseBody = AlexaResponseBody(outputSpeech = outputSpeech, shouldEndSession = response.second)
        return AlexaResponse(response = responseBody)
    }
}
