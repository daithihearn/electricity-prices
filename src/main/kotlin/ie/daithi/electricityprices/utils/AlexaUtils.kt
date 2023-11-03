package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.alexa.AlexaResponse
import java.time.LocalDateTime

fun wrapInSkillResponse(
    message: String,
    title: String,
    dateTime: LocalDateTime = LocalDateTime.now(),
): AlexaResponse {
    return AlexaResponse(
        updateDate = dateTime.format(alexaSkillFormatter),
        titleText = title,
        mainText = message
    )
}