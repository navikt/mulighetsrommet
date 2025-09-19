package no.nav.mulighetsrommet.api.validation

import arrow.core.*
import arrow.core.raise.*
import arrow.core.raise.either
import no.nav.mulighetsrommet.api.responses.FieldError
import kotlin.contracts.contract

class ValidationDsl(
    private val raise: Raise<List<FieldError>>,
) {
    val errors = mutableListOf<FieldError>()

    fun validate(condition: Boolean, error: () -> FieldError) {
        if (!condition) errors += error()
    }

    fun <A> validateNotNull(value: A?, error: () -> FieldError) {
        if (value == null) errors += error()
    }

    @kotlin.contracts.ExperimentalContracts
    fun requireValid(condition: Boolean, error: (() -> FieldError)? = null) {
        contract { returns() implies (condition) }
        if (!condition) {
            if (error != null) errors.add(error())
            raise.raise(errors)
        }
    }

    fun <A> Either<List<FieldError>, A>.bind(): A = when (this) {
        is Either.Left -> {
            errors.addAll(this.value)
            raise.raise(errors)
        }
        is Either.Right -> this.value
    }
}

fun <V> validation(block: ValidationDsl.() -> V): Either<List<FieldError>, V> = either {
    val dsl = ValidationDsl(this)
    val result = dsl.block()
    return dsl.errors.toNonEmptyListOrNull()?.left() ?: result.right()
}
