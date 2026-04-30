package no.nav.mulighetsrommet.api.clients.helved

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Utbetaling mot Hel Ved - utbetalingskontrakt inngått mellom teamene
 *
 * Dette er engangsutbetalinger (periodetype: EN_GANG) med månedlig motregning
 */
@Serializable
data class HelVedUtbetaling(
    /** Unik identifikator for utbetalingen */
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    /**
     * ID til saken i fagsystemet/vedtaksløsning som utbetalingen er en del av.
     */
    val sakId: String,
    /**
     * Id på behandlingen i fagsystemet/vedtaksløsning.
     * Kan brukes for å spore utbetalingen tilbake til den konkrete hendelsen som trigget den. Må fornyes for hver melding mot hel ved.
     */
    val behandlingId: String,
    /** Fødselsnummer eller D-nummer til personen som skal motta utbetalingen */
    val personident: NorskIdent,
    /** Utbetalingsperiode */
    val periode: Periode,
    /** Utbetalingslinjer som inngår i utbetalingen */
    val belop: Int,
    val tilskuddstype: Tilskuddstype,
    /** Saksbehandleren som har opprettet utbetalingen */
    val saksbehandler: NavIdent,
    /** Beslutteren som har attestert utbetalingen */
    val beslutter: NavIdent,
    /** Tidspunkt for besluttelse. Bruker ISO 8601-format. */
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant,
    /** Tiltakstypen som utbetalingen gjelder for */
    val tiltaksType: Tiltakskode,
    /** Bestemmer om utbetalingen er en simulering */
    val dryrun: Boolean,
) {
    init {
        // Kan kanskje flytte dette inn i en validator
        require(1 <= sakId.length && sakId.length <= 25) { "På grunn av begrensninger i OS/UR kan ikke sakId være lengre enn 25 tegn" }
        require(1 <= behandlingId.length && behandlingId.length <= 30) { "På grunn av begrensninger i OS/UR kan ikke denne være lengre enn 30 tegn" }
        require(belop > 0) { "Beløp kan ikke være negativt eller 0" }
        require(periode.fom.year == periode.tom.year) { "Utbetalingsperioden må være innen samme år" }
        require(periode.fom <= periode.tom) { "Fom-dato på en periode må være før eller lik tom-dato" }
    }

    @Serializable
    data class Periode(
        @Serializable(with = LocalDateSerializer::class)
        val fom: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val tom: LocalDate,
    )

    /** Se Tiltaksforskriften § 7-2 */
    @Serializable
    enum class Tiltakskode {
        /** Arena **/
        ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        ENKELTPLASS_FAG_OG_YRKESOPPLAERING,

        /** § 7-2 a */
        ARBEIDSMARKEDSOPPLAERING,

        /** § 7-2 b */
        NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,

        /** § 7-2 c */
        STUDIESPESIALISERING,

        /** § 7-2 d */
        FAG_OG_YRKESOPPLAERING,

        /** § 7-2 e */
        HOYERE_YRKESFAGLIG_UTDANNING,

        /** § 7-2 f */
        HOYERE_UTDANNING,
    }

    /** Se Tiltaksforskriften § 7-5 */
    @Serializable
    enum class Tilskuddstype {
        SKOLEPENGER,
        STUDIEREISE,
        EKSAMENSGEBYR,
        SEMESTERAVGIFT,
        INTEGRERT_BOTILBUD,
    }
}
