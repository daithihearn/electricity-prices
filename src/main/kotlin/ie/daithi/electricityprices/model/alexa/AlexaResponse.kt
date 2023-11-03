package ie.daithi.electricityprices.model.alexa

import ie.daithi.electricityprices.utils.alexaSkillFormatter
import java.time.LocalDateTime
import java.util.UUID

data class AlexaResponse(
    val uid: String = UUID.randomUUID().toString(),
    val updateDate: String = LocalDateTime.now().format(alexaSkillFormatter),
    val titleText: String,
    val mainText: String,
    val redirectionUrl: String = "https://elec.daithiapp.com/"
)
