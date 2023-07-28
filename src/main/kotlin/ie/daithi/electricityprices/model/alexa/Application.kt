package ie.daithi.electricityprices.model.alexa

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Application {
    var applicationId: String? = null
}