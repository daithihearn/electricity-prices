package ie.daithi.electricityprices.model.alexa

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Session {
    var sessionId: String? = null
    var application: Application? = null
    var attributes: Map<String, Any>? = null
    var user: User? = null
}
