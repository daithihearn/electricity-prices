package ie.daithi.electricityprices.model.alexa

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class User {
    var userId: String? = null
    var accessToken: String? = null
}