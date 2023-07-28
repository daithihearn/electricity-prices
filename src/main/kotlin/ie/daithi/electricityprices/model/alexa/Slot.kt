package ie.daithi.electricityprices.model.alexa

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Slot {
    var name: String? = null
    var value: String? = null
}