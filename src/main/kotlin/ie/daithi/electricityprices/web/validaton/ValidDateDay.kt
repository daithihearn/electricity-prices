package ie.daithi.electricityprices.web.validaton

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DateDayValidator::class])
@MustBeDocumented
annotation class ValidDateDay(
    val message: String =
        "The provided date is invalid. It must match yyyy-MM-dd",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class DateDayValidator : ConstraintValidator<ValidDateDay, String> {
    override fun isValid(
        value: String?,
        constraintValidatorContext: ConstraintValidatorContext
    ): Boolean {
        if (value == null) return true
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            LocalDate.parse(value, formatter)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }
}
