package no.nav.mulighetsrommet.admin.enhetsregister

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate

/**
 * En virksomhet (bedrift/selskap) slik den er kjent i "enhetsregisteret" - vår generaliserte
 * modell av Brreg sitt Enhetsregister, uavhengig av om virksomheten faktisk er registrert der
 * eller er en manuelt registrert utenlandsk virksomhet.
 */
sealed interface Virksomhet {
    val organisasjonsnummer: Organisasjonsnummer
    val navn: String
    val organisasjonsform: String?
    val slettetDato: LocalDate?

    @Serializable
    data class Hovedenhet(
        override val organisasjonsnummer: Organisasjonsnummer,
        override val navn: String,
        override val organisasjonsform: String?,
        val overordnetEnhet: Organisasjonsnummer? = null,
        @Serializable(with = LocalDateSerializer::class)
        override val slettetDato: LocalDate? = null,
    ) : Virksomhet

    @Serializable
    data class Underenhet(
        override val organisasjonsnummer: Organisasjonsnummer,
        override val navn: String,
        override val organisasjonsform: String?,
        val overordnetEnhet: Organisasjonsnummer? = null,
        @Serializable(with = LocalDateSerializer::class)
        override val slettetDato: LocalDate? = null,
    ) : Virksomhet
}

sealed interface VirksomhetOppslag {
    data class Funnet(val virksomhet: Virksomhet) : VirksomhetOppslag

    data class FjernetAvJuridiskeArsaker(
        val organisasjonsnummer: Organisasjonsnummer,
        val slettetDato: LocalDate,
    ) : VirksomhetOppslag
}
