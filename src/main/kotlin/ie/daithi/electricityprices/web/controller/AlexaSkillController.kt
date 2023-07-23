package ie.daithi.electricityprices.web.controller

import ie.daithi.electricityprices.model.AlexaSkillResponse
import ie.daithi.electricityprices.service.AlexSkillService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.enums.ParameterIn
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.Locale

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alexa", description = "Endpoints that relate to the electricity prices alexa skill")
class AlexaSkillController(private val alexSkillService: AlexSkillService) {

    @GetMapping("/alexa")
    @ResponseStatus(value = HttpStatus.OK)
    @Operation(summary = "Get info for the alexa skill", description = "Returns the JSON message feed for the electricity prices alexa skill")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Request successful")
    )
    @ResponseBody
    fun getFeed(@RequestParam(value = "locale", required = false) locale: String?): List<AlexaSkillResponse> {
        val resolvedLocale = locale?.let { Locale.forLanguageTag(it) } ?: Locale.forLanguageTag("es")
        return alexSkillService.getResponses(resolvedLocale)
    }

}
