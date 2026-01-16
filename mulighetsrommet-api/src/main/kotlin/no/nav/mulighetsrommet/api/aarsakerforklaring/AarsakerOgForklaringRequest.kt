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
        if ("ANNET" in aarsaker.map { it.toString() }) {
            validate(!forklaring.isNullOrBlank()) {
                FieldError("/aarsaker", "Beskrivelse er obligatorisk når “Annet” er valgt som årsak")
            }
        }

        validate(forklaring == null || forklaring.length <= FORKLARING_MAX_LENGTH) {
            FieldError(
                "/forklaring",
                "Beskrivelse kan ikke inneholde mer enn $FORKLARING_MAX_LENGTH tegn",
            )
        }

        validate(aarsaker.isNotEmpty()) {
            FieldError("/aarsaker", "Du må velge minst én årsak")
        }
    }.map { this }
}
