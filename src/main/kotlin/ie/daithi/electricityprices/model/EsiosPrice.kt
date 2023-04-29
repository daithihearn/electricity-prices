package ie.daithi.electricityprices.model

import com.fasterxml.jackson.annotation.JsonProperty

data class EsioPVPC (
    @JsonProperty("Dia")
    val day: String,
    @JsonProperty("Hora")
    val hour: String,
    @JsonProperty("PCB")
    val pcb: String?,
    @JsonProperty("GEN")
    val gen: String?,

)
data class EsiosPrice(
    @JsonProperty("PVPC")
    val pvpc: List<EsioPVPC>? = null,
)