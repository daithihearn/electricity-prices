package ie.daithi.electricityprices.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


@ResponseStatus(value = HttpStatus.NOT_FOUND)
class DataNotAvailableYetException(message: String) : RuntimeException(message)