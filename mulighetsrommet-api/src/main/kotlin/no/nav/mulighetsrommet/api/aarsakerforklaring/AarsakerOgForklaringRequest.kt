package no.nav.mulighetsrommet.api.aarsakerforklaring

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.responses.FieldError

@Serializable
data class AarsakerOgForklaringRequest<T>(
    val aarsaker: List<T>,
    val forklaring: String?,
)

fun <T> validateAarsakerOgForklaring(
    aarsaker: List<T>,
    forklaring: String?,
): Either<List<FieldError>, Unit> = either {
    val errors = buildList {
        if ("ANNET" in aarsaker.map { it.toString() } && forklaring.isNullOrBlank()) {
            add(FieldError.ofPointer("/aarsaker", "Beskrivelse er obligatorisk når “Annet” er valgt som årsak"))
        }
        if (forklaring != null && forklaring.length > 100) {
            add(FieldError.ofPointer("/forklaring", "Beskrivelse kan ikke inneholde mer enn 100 tegn"))
        }
    }
    if (errors.isNotEmpty()) {
        return errors.left()
    }
}
