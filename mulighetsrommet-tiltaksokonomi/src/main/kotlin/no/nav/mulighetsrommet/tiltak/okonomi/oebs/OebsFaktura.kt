package no.nav.mulighetsrommet.tiltak.okonomi.oebs

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.tiltak.okonomi.oebs.OebsFakturaMelding.Linje
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class OebsFakturaMelding(
    /**
     * Unik ID for fakturaen og benyttes som primærnøkkel i kommunikasjonen med OeBS.
     */
    val fakturaNummer: String,

    /**
     * Tidspunkt da fakturaen ble godkjent i en totrinnskontroll.
     */
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettelsesTidspunkt: LocalDateTime,

    /**
     * Indikerer hvilket fagsystem som er kilden til fakturaen.
     */
    val kilde: Kilde,

    /**
     * Organisasjonsnummer for hovedenhet til bedriften som det skal utbetales til.
     */
    val organisasjonsNummer: String,

    /**
     * Organisasjonsnummer for bedriften som det skal utbetales til.
     * Dette må være en underenhet av hovedenheten.
     */
    val bedriftsNummer: String,

    /**
     * Indikerer hvem som har opprettet og sendt fakturaen til godkjenning.
     * Dette benyttes til sporing og er bl.a. relevant ifm. revisjon.
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

    @Serializable(with = LocalDateSerializer::class)
    val fakturaDato: LocalDate,

    /**
     * Totalt beløp som skal utbetales.
     */
    val totalSum: Int,

    /**
     * Vil foreløpig alltid være NOK.
     */
    val valutaKode: String,

    /**
     * Kontonummer det skal utbetales til.
     */
    val bankKontoNummer: String,

    /**
     * [OebsBetalingskanal.BBAN] for utbetaling til norske banker, [OebsBetalingskanal.IBAN] ellers.
     */
    val betalingsKanal: OebsBetalingskanal,

    /**
     * Påkrev når [betalingsKanal] er [OebsBetalingskanal.IBAN], null ellers.
     */
    val bankNavn: String?,

    /**
     * Påkrev når [betalingsKanal] er [OebsBetalingskanal.IBAN], null ellers.
     */
    val bankLandKode: String?,

    /**
     * Påkrev når [betalingsKanal] er [OebsBetalingskanal.IBAN], null ellers.
     */
    val bicSwiftKode: String?,

    /**
     * KID er antagelig kun releveant når [betalingsKanal] er [OebsBetalingskanal.BBAN], men vi sender det alltid
     * med om det er definert.
     */
    val kidNummer: String?,

    /**
     * Melding til leverandør burde alltid beskrive hva fakturaen gjelder slik at mottaker skjønner hva innbetalingen
     * er for.
     *
     * TODO: Det er mulig denne kun burde settes når [kidNummer] er null. OeBS skulle finne ut av det.
     *
     * TODO: OeBS mente det kunne være relevant å begrense lengden på meldingen, muligens 140 tegn, men det er enda ikke avklart.
     */
    val meldingTilLeverandor: String?,

    /**
     * Det samme som [meldingTilLeverandor], men til bruk for feilsøking i OeBS.
     *
     * TODO: Er dette feltet fortsatt relevant?
     *
     * OeBS mente at dette feltet kanskje ikke lengre er relevant. En mulig bruk for å videreføre dette er om
     * [meldingTilLeverandor] ikke settes, men at utbetaling feiler f.eks. pga feil KID. Da kan man utbetale
     * manuelt fra OeBS ved å bruke [beskrivelse] som [meldingTilLeverandor].
     */
    val beskrivelse: String?,

    /**
     * En faktura må ha én eller flere linjer.
     *
     * Hver linje refererer til en linje i [OebsBestillingMelding.bestillingsLinjer] via kombinasjonen av
     * [Linje.bestillingsnummer] og [Linje.bestillingsLinjeNummer].
     *
     * Det er uklart om samme faktura kan referere til flere forskjellige bestillinger (via flere linjer) selv om
     * modellen tillater det.
     */
    val fakturaLinjer: List<Linje>,
) {
    @Serializable
    data class Linje(
        /**
         * Referanse til bestillingen som linjen refererer til.
         */
        val bestillingsnummer: String,

        /**
         * Referanse til linje innad i bestillingen.
         */
        val bestillingsLinjeNummer: Int,

        /**
         * Antall enheter som det skal utbetales for. Total pris for en linje er [antall] * [pris].
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
         * Hvis dette flagget settes så blir evt. resterende beløp fra bestillingen frigjort.
         *
         * Det er viktig at fakturaer behandles i riktig rekkefølge hos OeBS, så dette flagget må kun settes på den
         * siste fakturaen for en bestilling og det må ikke overføres til OeBS før alle tidligere fakturaer har
         * blitt mottatt (eller prosessert?).
         *
         * TODO: må vi sjekke status på tidligere fakturaer også, eller holder det å vite at de er mottatt?
         */
        val erSisteFaktura: Boolean,
    )
}

enum class OebsBetalingskanal {
    BBAN,
    IBAN,
}
