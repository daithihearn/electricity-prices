package ie.daithi.electricityprices.model.alexa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import java.util.Map

@JsonIgnoreProperties(ignoreUnknown = true)
class AlexaIntent {
    var name: String? = null
    var slots: Map<String, Slot>? = null
}
