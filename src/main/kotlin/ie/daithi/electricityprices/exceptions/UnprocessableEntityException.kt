package ie.daithi.electricityprices.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
class UnprocessableEntityException(message: String) : RuntimeException(message)