package ie.daithi.electricityprices.model.alexa

data class AlexaResponseBody(
    val outputSpeech: OutputSpeech,
    val shouldEndSession: Boolean
)
