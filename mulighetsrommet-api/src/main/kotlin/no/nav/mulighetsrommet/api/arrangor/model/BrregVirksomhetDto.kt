package no.nav.mulighetsrommet.api.arrangor.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class BrregVirksomhetDto(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhet: Organisasjonsnummer? = null,
    val underenheter: List<BrregVirksomhetDto>? = null,
    val postnummer: String?,
    val poststed: String?,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)
