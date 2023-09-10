package ie.daithi.electricityprices.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import jakarta.validation.ConstraintViolationException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        e: ConstraintViolationException,
        webRequest: WebRequest
    ): ResponseEntity<Map<String, Any>> {
        val errorAttributes = mutableMapOf<String, Any>()
        errorAttributes["timestamp"] = System.currentTimeMillis()
        errorAttributes["status"] = HttpStatus.UNPROCESSABLE_ENTITY.value()
        errorAttributes["error"] = "Unprocessable Entity"
        errorAttributes["message"] = e.message ?: "Validation failed"
        errorAttributes["path"] = (webRequest as ServletWebRequest).request.requestURI
        return ResponseEntity(errorAttributes, HttpStatus.UNPROCESSABLE_ENTITY)
    }
}
