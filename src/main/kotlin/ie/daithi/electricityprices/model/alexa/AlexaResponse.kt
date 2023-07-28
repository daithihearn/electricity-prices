package ie.daithi.electricityprices.model.alexa

data class AlexaResponse(
    val version: String = "1.0",
    val response: AlexaResponseBody
)
