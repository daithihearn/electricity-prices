package ie.daithi.electricityprices.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PriceValues (
    val value: Double,
    val percentage: Double,
    val datetime: String
)

data class Attributes(
    val title: String,
    val description: String? = null,
    val color: String? = null,
    val type: String? = null,
    val magnitude: String? = null,
    val composite: Boolean,
    @JsonProperty("last-update")
    val lastUpdate: String,
    val values: List<PriceValues>
)

data class Included(
    val attributes: Attributes,
    val id: String,
    val type: String,
    val groupId: String? = null
)

data class ReePrice(
    val data: Any,
    val included: List<Included>,
)