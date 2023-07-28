package ie.daithi.electricityprices.model.alexa

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class AlexaRequest {
    var version: String? = null
    var session: Session? = null
    var request: Request? = null
}
