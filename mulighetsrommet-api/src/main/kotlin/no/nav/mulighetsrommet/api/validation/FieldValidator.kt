package no.nav.mulighetsrommet.api.validation

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.mulighetsrommet.api.responses.FieldError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KProperty1

typealias Validated<T> = Either<List<FieldError>, T>

@DslMarker
annotation class FieldValidatorDsl

@FieldValidatorDsl
class FieldValidator(
    private val raise: Raise<List<FieldError>>,
) {
    val errors = mutableListOf<FieldError>()

    fun <V> path(
        vararg path: KProperty1<*, *>,
        block: FieldValidator.() -> V,
    ): V {
        val result = validation { block() }
        return when (result) {
            is Either.Left -> {
                result.value.forEach { error { it.withParent(*path) } }
                raise.raise(errors)
            }

            is Either.Right -> result.value
        }
    }

    fun error(error: () -> FieldError) {
        errors.add(error())
    }

    fun validate(condition: Boolean, error: () -> FieldError) {
        if (!condition) errors += error()
    }

    fun <A> validateNotNull(value: A?, error: () -> FieldError) {
        if (value == null) errors += error()
    }

    @ExperimentalContracts
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

fun <V> validation(vararg path: KProperty1<*, *>, block: FieldValidator.() -> V): Either<List<FieldError>, V> = either {
    val dsl = FieldValidator(this)
    val result = if (path.isNotEmpty()) dsl.path(*path) { block() } else dsl.block()
    return dsl.errors.toNonEmptyListOrNull()?.left() ?: result.right()
}
