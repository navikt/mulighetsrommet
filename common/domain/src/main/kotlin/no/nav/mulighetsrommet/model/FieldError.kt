package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

@Serializable
data class FieldError(
    val pointer: String,
    val detail: String,
) {
    companion object {
        fun of(detail: String, vararg property: KProperty1<*, *>): FieldError {
            return FieldError(
                pointer = property.joinToString(prefix = "/", separator = "/") { it.name },
                detail = detail,
            )
        }
    }

    fun withParent(vararg property: KProperty1<*, *>): FieldError {
        val parentPointer = if (property.isNotEmpty()) {
            property.joinToString(prefix = "/", separator = "/") { it.name } + pointer
        } else {
            pointer
        }
        return FieldError(parentPointer, detail)
    }
}
