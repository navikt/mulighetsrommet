package no.nav.mulighetsrommet.ereg

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

sealed class EregEnhet {
    abstract val organisasjonsnummer: Organisasjonsnummer
    abstract val organisasjonsform: String?
    abstract val navn: String
}

/**
 * Juridisk enhet eller organisasjonsledd i Ereg, tilsvarer en hovedenhet i Brreg.
 */
sealed class EregHovedenhet : EregEnhet() {
    abstract val postadresse: EregAdresse?
    abstract val forretningsadresse: EregAdresse?
}

/**
 * Virksomhet i Ereg, tilsvarer en underenhet i Brreg.
 */
sealed class EregUnderenhet : EregEnhet()

@Serializable
data class EregHovedenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String?,
    override val navn: String,
    override val postadresse: EregAdresse?,
    override val forretningsadresse: EregAdresse?,
    val overordnetEnhet: Organisasjonsnummer?,
) : EregHovedenhet()

@Serializable
data class SlettetEregHovedenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String?,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate,
) : EregHovedenhet() {
    override val postadresse: EregAdresse? = null
    override val forretningsadresse: EregAdresse? = null
}

@Serializable
data class EregUnderenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String?,
    override val navn: String,
    val overordnetEnhet: Organisasjonsnummer?,
) : EregUnderenhet()

@Serializable
data class SlettetEregUnderenhetDto(
    override val organisasjonsnummer: Organisasjonsnummer,
    override val organisasjonsform: String?,
    override val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val slettetDato: LocalDate,
) : EregUnderenhet()

@Serializable
data class EregAdresse(
    val landkode: String?,
    val postnummer: String?,
    val poststed: String?,
    val adresse: List<String>?,
)
