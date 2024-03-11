package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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

// TODO Mulig virksometene burde vært en sealed class slik at vi enklere visste om det var en hovedenhet eller underenhet?
@Serializable
data class LagretVirksomhetDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null,
    val underenheter: List<LagretVirksomhetDto>? = null,
    val postnummer: String?,
    val poststed: String?,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate? = null,
)
