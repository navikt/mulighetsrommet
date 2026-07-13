package no.nav.mulighetsrommet.api.domain.arrangor

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

sealed class Arrangor {
    abstract val id: UUID
    abstract val organisasjonsnummer: Organisasjonsnummer
    abstract val organisasjonsform: String?
    abstract val navn: String
    abstract val overordnetEnhet: Organisasjonsnummer?
    abstract val slettetDato: LocalDate?
    abstract val kontaktpersoner: List<ArrangorKontaktperson>

    fun medKontaktpersoner(kontaktpersoner: List<ArrangorKontaktperson>): Arrangor = when (this) {
        is Norsk -> copy(kontaktpersoner = kontaktpersoner)
        is Utenlandsk -> copy(kontaktpersoner = kontaktpersoner)
    }

    /**
     * Norske virksomheter hentet fra Brønnøysundregisteret. Betalingsinformasjon er ikke lagret direkte på
     * arrangør, men hentes i stedet on demand fra utbetalingsseksjonen (Kontoregister Organisasjon).
     */
    data class Norsk(
        override val id: UUID,
        override val organisasjonsnummer: Organisasjonsnummer,
        override val organisasjonsform: String?,
        override val navn: String,
        override val overordnetEnhet: Organisasjonsnummer? = null,
        override val slettetDato: LocalDate? = null,
        override val kontaktpersoner: List<ArrangorKontaktperson> = emptyList(),
    ) : Arrangor()

    /**
     * Utenlandske virksomheter som er registrert manuelt og arvet fra Arena.
     *
     * "Organisasjonsnummeret" er fiktivt og begynner på "1". Dette har opprettet i Arena og blitt arvet
     * til Tiltaksadministrasjon. En (eller eneste?) av årsakene til at det har blitt definert et fiktivt
     * organisasjonsnummer er at utbetalingsseksjonen (OeBS) krever/benytter dette som hovedidentifikator
     * for bedrifter ifm. utbetalinger.
     *
     * For at utenlandske virksomheter skal kunne motta utbetalinger må betalingsinformasjonen og adresse
     * registreres manuelt i eget register (altså egen database).
     */
    data class Utenlandsk(
        override val id: UUID,
        override val organisasjonsnummer: Organisasjonsnummer,
        override val organisasjonsform: String?,
        override val navn: String,
        override val overordnetEnhet: Organisasjonsnummer? = null,
        override val slettetDato: LocalDate? = null,
        override val kontaktpersoner: List<ArrangorKontaktperson> = emptyList(),
        val betalingsinformasjon: Betalingsinformasjon.IBan? = null,
        val adresse: Adresse? = null,
    ) : Arrangor() {
        data class Adresse(
            val gateNavn: String,
            val by: String,
            val postNummer: String,
            val landKode: String,
        )
    }
}
