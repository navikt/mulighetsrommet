package no.nav.mulighetsrommet.brreg

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

sealed class BrregVirksomhet {
    abstract val organisasjonsnummer: Organisasjonsnummer
    abstract val navn: String
}

sealed class BrregEnhet : BrregVirksomhet()

sealed class BrregUnderenhet : BrregVirksomhet()

@Serializable
data class BrregEnhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: String,
    val postnummer: String?,
    val poststed: String?,
) : BrregEnhet()

@Serializable
data class BrregEnhetMedUnderenheterDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: String,
    val underenheter: List<BrregUnderenhetDto>,
    val postnummer: String?,
    val poststed: String?,
) : BrregEnhet()

@Serializable
data class SlettetBrregEnhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate,
) : BrregEnhet()

@Serializable
data class BrregUnderenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: String,
    val overordnetEnhet: Organisasjonsnummer,
    val postnummer: String?,
    val poststed: String?,
) : BrregUnderenhet()

@Serializable
data class SlettetBrregUnderenhet(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate,
) : BrregUnderenhet()
