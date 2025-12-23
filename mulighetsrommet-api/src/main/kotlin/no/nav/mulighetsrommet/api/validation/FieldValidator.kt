package no.nav.mulighetsrommet.api.validation

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.mulighetsrommet.api.responses.FieldError
import kotlin.contracts.contract

@DslMarker
annotation class FieldValidatorDsl

@FieldValidatorDsl
class FieldValidator(
    private val raise: Raise<List<FieldError>>,
) {
    val errors = mutableListOf<FieldError>()

    fun error(error: () -> FieldError) {
        errors.add(error())
    }

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

fun <V> validation(block: FieldValidator.() -> V): Either<List<FieldError>, V> = either {
    val dsl = FieldValidator(this)
    val result = dsl.block()
    return dsl.errors.toNonEmptyListOrNull()?.left() ?: result.right()
}
