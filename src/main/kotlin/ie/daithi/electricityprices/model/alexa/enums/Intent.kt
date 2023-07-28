package ie.daithi.electricityprices.model.alexa.enums

enum class Intent(val value: String) {
    CANCEL("AMAZON.CancelIntent"),
    HELP("AMAZON.HelpIntent"),
    STOP("AMAZON.StopIntent"),
    NAVIGATE_HOME("AMAZON.NavigateHomeIntent"),
    FALLBACK("AMAZON.FallbackIntent"),
    FULL("FULL"),
    TODAY("TODAY"),
    TOMORROW("TOMORROW"),
    NEXT_CHEAP("NEXT_CHEAP"),
    NEXT_EXPENSIVE("NEXT_EXPENSIVE"),
}