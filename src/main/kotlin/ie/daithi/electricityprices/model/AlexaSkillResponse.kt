package ie.daithi.electricityprices.model

import java.time.LocalDateTime
import java.util.UUID

data class AlexaSkillResponse(
    val uid: String = UUID.randomUUID().toString(),
    val updateDate: String = LocalDateTime.now().toString(),
    val titleText: String,
    val mainText: String
)
