package no.nav.mulighetsrommet.brreg

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

sealed class BrregEnhet {
    /**
     * Nisifret nummer som entydig identifiserer virksomheter i Enhetsregisteret, både hovedenheter,
     * organisasjonsledd og underenheter.
     */
    abstract val organisasjonsnummer: Organisasjonsnummer

    /**
     * Inndeling av virksomheter ut fra hvordan disse er organisert (eierform, ansvarsforhold, regelverk og lignende).
     *
     * https://www.brreg.no/bedrift/organisasjonsformer/
     */
    abstract val organisasjonsform: String

    /**
     * Navn på virksomhet som er registrert i Enhetsregisteret.
     */
    abstract val navn: String
}

/**
 * Hovedenhet eller organisasjonsledd i Enhetsregisteret.
 */
sealed class BrregHovedenhet : BrregEnhet()

/**
 * Underenhet i Enhetsregisteret.
 */
sealed class BrregUnderenhet : BrregEnhet()

@Serializable
data class BrregHovedenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String,
    override val navn: String,
    val postnummer: String?,
    val poststed: String?,
) : BrregHovedenhet()

@Serializable
data class BrreHovedenhetMedUnderenheterDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String,
    override val navn: String,
    val underenheter: List<BrregUnderenhetDto>,
    val postnummer: String?,
    val poststed: String?,
) : BrregHovedenhet()

@Serializable
data class SlettetBrregHovedenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate,
) : BrregHovedenhet()

@Serializable
data class BrregUnderenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String,
    override val navn: String,
    val overordnetEnhet: Organisasjonsnummer,
    val postnummer: String?,
    val poststed: String?,
) : BrregUnderenhet()

@Serializable
data class SlettetBrregUnderenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate,
) : BrregUnderenhet()
