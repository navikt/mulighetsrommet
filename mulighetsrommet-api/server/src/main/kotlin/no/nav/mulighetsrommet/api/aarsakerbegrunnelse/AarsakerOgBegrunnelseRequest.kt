package no.nav.mulighetsrommet.api.aarsakerbegrunnelse

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.validation.validation

private const val BEGRUNNELSE_MAX_LENGTH = 500

@Serializable
data class AarsakerOgBegrunnelseRequest<T>(
    val aarsaker: List<T>,
    val begrunnelse: String?,
) {
    fun validate(): Either<List<FieldError>, AarsakerOgBegrunnelseRequest<T>> = validation {
        if ("ANNET" in aarsaker.map { it.toString() }) {
            validate(!begrunnelse.isNullOrBlank()) {
                FieldError("/aarsaker", "Beskrivelse er obligatorisk når “Annet” er valgt som årsak")
            }
        }

        validate(begrunnelse == null || begrunnelse.length <= BEGRUNNELSE_MAX_LENGTH) {
            FieldError(
                "/begrunnelse",
                "Beskrivelse kan ikke inneholde mer enn $BEGRUNNELSE_MAX_LENGTH tegn",
            )
        }

        validate(aarsaker.isNotEmpty()) {
            FieldError("/aarsaker", "Du må velge minst én årsak")
        }
    }.map { this }
}
