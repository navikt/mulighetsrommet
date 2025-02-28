package no.nav.tiltak.okonomi.oebs

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDate
import java.time.LocalDateTime

enum class OebsBestillingType {
    NY,
    ANNULLER,
}

@Serializable
data class OebsBestillingMelding(
    /**
     * Unik ID for bestillingen og benyttes som primærnøkkel i kommunikasjonen med OeBS.
     */
    val bestillingsNummer: String,

    /**
     * Ønske fra OeBS (via riksrevisjonen?) for å ha sporing mellom inngått rammeavtale og utbetaling.
     * Hvis valp sender med rammeavtalenummer så kan oebs lagre denne koblingen
     * og oebs kan ta ut rapporter på hvor mye som har blitt betalt ihht. en rammeavtale
     */
    val rammeavtaleNummer: String?,

    /**
     * Tidspunkt da bestillingen (tilsagnet) ble godkjent i en totrinnskontroll.
     */
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,

    /**
     * Indikerer hvilket fagsystem som er kilden til bestillingen.
     */
    val kilde: OebsKilde,

    /**
     * Totalt beløp for bestillingen.
     * Alltid hele kroner.
     */
    val totalSum: Int,

    /**
     * Alltid [OebsBestillingType.NY] for opprettelse av ny bestilling (tilsagn).
     */
    val bestillingsType: OebsBestillingType,

    /**
     * Indikerer hvem som har opprettet og sendt bestillingen (tilsagnet) til godkjenning.
     *
     * Nav-ident hvis det er en person, evt. systemnavn hvis det er et system.
     */
    val saksbehandler: String,

    /**
     * Indikerer hvem som har godkjent fakturaen. Dette benyttes til sporing og er bl.a. relevant ifm. revisjon.
     * Personer må ha budsjettsdisponeringsmyndighet (bdm) for å kunne godkjenne en faktura.
     *
     * Nav-ident hvis det er en person, evt. systemnavn hvis det er et system.
     */
    val bdmGodkjenner: String,

    /**
     * Startdato for bestillingen (tilsagnet).
     */
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,

    /**
     * Sluttdato for bestillingen (tilsagnet).
     */
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate,

    /**
     * Vil foreløpig alltid være NOK.
     */
    val valutaKode: String,

    /**
     * Bedriften som det skal utbetales til.
     */
    val selger: Selger,

    /**
     * En bestilling består av én eller flere linjer.
     * Hver linje må representerer en periode og må alltid innholde seg innenfor en måned.
     */
    val bestillingsLinjer: List<Linje>,

    /**
     * Konteringsinformasjon - benyttes for å utlede PA prosjekt + kontering på PO-bestillingen
     *
     * 12 siffer (Kapittel/Underpost)
     */
    val statsregnskapsKonto: String,

    /**
     * Konteringsinformasjon – benytter kontering på PO-bestillingen
     *
     * 12 siffer (Type tiltak)
     */
    val artsKonto: String,

    /**
     * Konteringsinformasjon - Nav-enhet som skal belastes for bestillingen (tilsagnet).
     *
     * Nav-enhet/kontor representeres av et 4-sifret tall (fra Norg) og mappes til et kostnadssted innad i OeBS.
     * Det må være enighet mellom fagsystet og OeBS om hvilke kostnadssteder som skal benyttes.
     */
    val kontor: String,

    /**
     * Konteringsinformasjon - det året tilsagnet gjelder for.
     *
     * Et tilsagn kan aldri vare over et årsskifte.
     *
     * Benyttes for å utlede PA prosjekt + kontering på PO-bestillingen.
     */
    val tilsagnsAar: Int,
) {
    @Serializable
    data class Selger(
        /**
         * Organisasjonsnummer for hovedenhet til bedriften som det skal utbetales til.
         */
        val organisasjonsNummer: String,

        /**
         * Navn på hovedenhet til bedriften som det skal utbetales til.
         *
         * OeBS benytter dette til å opprette eller oppdatere bedriften internt.
         * Merk at forskjellige produsenter av bestillinger til OeBS kan overskrive denne informasjonen.
         *
         * TODO: Fjern om OeBS får implementert denne funksjonaliteten selv.
         */
        val organisasjonsNavn: String,

        /**
         * Adresse til hovedenheten til bedriften som det skal utbetales til.
         *
         * OeBS benytter dette til å opprette eller oppdatere bedriften internt.
         * Merk at forskjellige produsenter av bestillinger til OeBS kan overskrive denne informasjonen.
         *
         * TODO: Fjern om OeBS får implementert denne funksjonaliteten selv.
         */
        val adresse: List<Adresse>,

        /**
         * Organisasjonsnummer for bedriften som det skal utbetales til.
         * Dette må være en underenhet av hovedenheten.
         */
        val bedriftsNummer: String,
    ) {
        @Serializable
        data class Adresse(
            val gateNavn: String,
            val by: String,
            val postNummer: String,
            val landsKode: String,
        )
    }

    @Serializable
    data class Linje(
        /**
         * Indeks for linjen i bestillingen, starter på 1.
         */
        val linjeNummer: Int,

        /**
         * Antall enheter som bestilles. Total pris for en linje er [antall] * [pris].
         *
         * For å tillate flere del-utbetaling av bestillinger (tilsagn) så settes pris alltid til 1, mens antallet
         * settes til det som ellers ville vært totalbeløpet for en periode (linje).
         *
         * Hvis man hadde gjort det motsatte, altså satt antall til 1 og pris til totalbeløpet, så ville man "brukt opp"
         * tilaket (antallet) ved første utbetaling.
         */
        val antall: Int,

        /**
         * Pris per enhet. Total pris for en linje er [antall] * [pris].
         *
         * Alltid satt til 1 for tiltaksøkonomien.
         */
        val pris: Int = 1,

        /**
         * Hvilken måned gjelder dette for?
         * "01" = januar, "02" = februar, ..., "12" = desember
         */
        val periode: String,

        /**
         * Alltid i samme måned som periode.
         */
        @Serializable(with = LocalDateSerializer::class)
        val startDato: LocalDate,

        /**
         * Alltid i samme måned som periode.
         */
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate,

        /**
         * Alltid 900000 for tiltak. Dette er oebs sitt artikkelnummer for tiltak
         */
        val artikkelNummer: String = "900000",

        /**
         * Mva, alltid 00 for tiltak
         */
        val avgiftsKode: String = "00",
    )
}

@Serializable
data class OebsAnnulleringMelding(
    /**
     * Refererer til en bestilling som først må ha blitt opprettet via en [OebsBestillingMelding].
     */
    val bestillingsNummer: String,

    /**
     * Alltid [OebsBestillingType.ANNULLER]for annullering av bestillinger (tilsagn).
     */
    val bestillingsType: OebsBestillingType,

    /**
     * Må være de samme verdiene som ble brukt i [OebsBestillingMelding], selv om en enhet potensielt kan ha
     * blitt nedlagt/erstattet i Brreg i perioden mellom en bestilling og en annullering.
     */
    val selger: Selger,
) {
    @Serializable
    data class Selger(
        /**
         * Organisasjonsnummer for hovedenhet til bedriften som det skal utbetales til.
         */
        val organisasjonsNummer: String,

        /**
         * Organisasjonsnummer for bedriften som det skal utbetales til.
         * Dette må være en underenhet av hovedenheten.
         */
        val bedriftsNummer: String,
    )
}
