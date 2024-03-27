package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class BrregVirksomhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null,
    val underenheter: List<BrregVirksomhetDto>? = null,
    val postnummer: String?,
    val poststed: String?,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)
