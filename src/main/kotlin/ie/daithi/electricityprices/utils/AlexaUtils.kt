package ie.daithi.electricityprices.utils

import ie.daithi.electricityprices.model.alexa.AlexaSkillResponse
import java.time.LocalDateTime

fun wrapInSkillResponse(
    message: String,
    title: String,
    dateTime: LocalDateTime = LocalDateTime.now(),
): AlexaSkillResponse {
    return AlexaSkillResponse(
        updateDate = dateTime.format(alexaSkillFormatter),
        titleText = title,
        mainText = message
    )
}