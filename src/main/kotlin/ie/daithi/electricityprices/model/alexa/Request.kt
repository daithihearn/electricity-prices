package ie.daithi.electricityprices.model.alexa

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Request {
    var type: String? = null
    var requestId: String? = null
    var timestamp: String? = null
    var intent: AlexaIntent? = null
}