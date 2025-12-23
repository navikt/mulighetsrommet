package no.nav.mulighetsrommet.api.aarsakerforklaring

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.validation

private const val FORKLARING_MAX_LENGTH = 500

@Serializable
data class AarsakerOgForklaringRequest<T>(
    val aarsaker: List<T>,
    val forklaring: String?,
) {
    fun validate(): Either<List<FieldError>, AarsakerOgForklaringRequest<T>> = validation {
        error("ANNET" in aarsaker.map { it.toString() } && forklaring.isNullOrBlank()) {
            FieldError.ofPointer("/aarsaker", "Beskrivelse er obligatorisk når “Annet” er valgt som årsak")
        }

        error(forklaring != null && forklaring.length > FORKLARING_MAX_LENGTH) {
            FieldError.ofPointer(
                "/forklaring",
                "Beskrivelse kan ikke inneholde mer enn $FORKLARING_MAX_LENGTH tegn",
            )
        }

        error(aarsaker.isEmpty()) {
            FieldError.ofPointer("/aarsaker", "Du må velge minst én årsak")
        }
    }.map { this }
}
