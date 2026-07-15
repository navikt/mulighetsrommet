package no.nav.mulighetsrommet.api.domain.arrangor

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

sealed class Arrangor {
    abstract val id: UUID
    abstract val organisasjonsnummer: Organisasjonsnummer
    abstract val navn: String
    abstract val slettetDato: LocalDate?
    abstract val kontaktpersoner: List<ArrangorKontaktperson>

    abstract fun registrerKontaktpersoner(kontaktpersoner: List<ArrangorKontaktperson>): Arrangor

    abstract fun registrerSlettet(slettetDato: LocalDate): Arrangor

    /**
     * Norske virksomheter hentet fra Brønnøysundregisteret (Brreg). Betalingsinformasjon er ikke lagret
     * direkte på arrangør, men hentes i stedet on demand fra utbetalingsseksjonen (Kontoregister Organisasjon).
     */
    data class Norsk private constructor(
        override val id: UUID,
        override val organisasjonsnummer: Organisasjonsnummer,
        val organisasjonsform: String?,
        override val navn: String,
        val overordnetEnhet: Organisasjonsnummer?,
        override val slettetDato: LocalDate?,
        override val kontaktpersoner: List<ArrangorKontaktperson>,
    ) : Arrangor() {
        override fun registrerKontaktpersoner(kontaktpersoner: List<ArrangorKontaktperson>): Norsk {
            return copy(kontaktpersoner = kontaktpersoner)
        }

        override fun registrerSlettet(slettetDato: LocalDate): Norsk {
            return copy(slettetDato = slettetDato)
        }

        fun registrerVirksomhet(navn: String, organisasjonsform: String?): Norsk {
            return copy(navn = navn, organisasjonsform = organisasjonsform)
        }

        companion object {
            fun opprett(
                id: UUID,
                organisasjonsnummer: Organisasjonsnummer,
                organisasjonsform: String?,
                navn: String,
                overordnetEnhet: Organisasjonsnummer? = null,
                slettetDato: LocalDate? = null,
            ) = Norsk(
                id = id,
                organisasjonsnummer = organisasjonsnummer,
                organisasjonsform = organisasjonsform,
                navn = navn,
                overordnetEnhet = overordnetEnhet,
                slettetDato = slettetDato,
                kontaktpersoner = listOf(),
            )

            fun fromStorage(
                id: UUID,
                organisasjonsnummer: Organisasjonsnummer,
                organisasjonsform: String?,
                navn: String,
                overordnetEnhet: Organisasjonsnummer?,
                slettetDato: LocalDate?,
                kontaktpersoner: List<ArrangorKontaktperson>,
            ) = Norsk(
                id = id,
                organisasjonsnummer = organisasjonsnummer,
                organisasjonsform = organisasjonsform,
                navn = navn,
                overordnetEnhet = overordnetEnhet,
                slettetDato = slettetDato,
                kontaktpersoner = kontaktpersoner,
            )
        }
    }

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
    data class Utenlandsk private constructor(
        override val id: UUID,
        override val organisasjonsnummer: Organisasjonsnummer,
        override val navn: String,
        override val slettetDato: LocalDate?,
        override val kontaktpersoner: List<ArrangorKontaktperson>,
        val betalingsinformasjon: Betalingsinformasjon.IBan?,
        val adresse: Adresse?,
    ) : Arrangor() {
        override fun registrerKontaktpersoner(kontaktpersoner: List<ArrangorKontaktperson>): Utenlandsk {
            return copy(kontaktpersoner = kontaktpersoner)
        }

        override fun registrerSlettet(slettetDato: LocalDate): Utenlandsk {
            return copy(slettetDato = slettetDato)
        }

        fun registrerBetalingsinformasjon(
            betalingsinformasjon: Betalingsinformasjon.IBan,
            adresse: Adresse,
        ): Utenlandsk {
            return copy(betalingsinformasjon = betalingsinformasjon, adresse = adresse)
        }

        data class Adresse(
            val gateNavn: String,
            val by: String,
            val postNummer: String,
            val landKode: String,
        )

        companion object {
            fun opprett(
                id: UUID,
                organisasjonsnummer: Organisasjonsnummer,
                navn: String,
                slettetDato: LocalDate? = null,
            ) = Utenlandsk(
                id = id,
                organisasjonsnummer = organisasjonsnummer,
                navn = navn,
                slettetDato = slettetDato,
                kontaktpersoner = listOf(),
                betalingsinformasjon = null,
                adresse = null,
            )

            fun fromStorage(
                id: UUID,
                organisasjonsnummer: Organisasjonsnummer,
                navn: String,
                slettetDato: LocalDate?,
                kontaktpersoner: List<ArrangorKontaktperson>,
                betalingsinformasjon: Betalingsinformasjon.IBan?,
                adresse: Adresse?,
            ) = Utenlandsk(
                id = id,
                organisasjonsnummer = organisasjonsnummer,
                navn = navn,
                slettetDato = slettetDato,
                kontaktpersoner = kontaktpersoner,
                betalingsinformasjon = betalingsinformasjon,
                adresse = adresse,
            )
        }
    }
}
