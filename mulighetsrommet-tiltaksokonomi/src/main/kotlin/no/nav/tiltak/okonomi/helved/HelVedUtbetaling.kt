package no.nav.tiltak.okonomi.helved

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.*

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
     * På grunn av begrensninger i OS/UR kan ikke denne være lengre enn 25 tegn.
     */
    val sakId: String,
    /**
     * Id på behandlingen i fagsystemet/vedtaksløsning.
     * Kan brukes for å spore utbetalingen tilbake til den konkrete hendelsen som trigget den.
     * DVH kan bruke denne når de sammenstiller tilskuddsstatistikk.
     * På grunn av begrensninger i OS/UR kan ikke denne være lengre enn 30 tegn. */
    val behandlingId: String,
    /** Fødselsnummer eller D-nummer til personen som skal motta utbetalingen */
    val personident: NorskIdent,
    /** Tidspunkt for besluttelse. Bruker ISO 8601-format. */
    @Serializable(with = InstantSerializer::class)
    val besluttetTidspunkt: Instant,
    /** Utbetalingslinjer som inngår i utbetalingen */
    val linjer: List<UtbetalingLinje>,
    /** Saksbehandleren som har opprettet utbetalingen */
    val saksbehandler: NavIdent,
    /** Beslutteren som har attestert utbetalingen */
    val beslutter: NavIdent,
    /** Tiltakstypen som utbetalingen gjelder for */
    val tiltaksType: Tiltakskode,
    /** Bestemmer om utbetalingen er en simulering */
    val dryrun: Boolean,
) {
    /** Se Tiltaksforskriften § 7-2 */
    @Serializable
    enum class Tiltakskode {
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

    @Serializable
    data class UtbetalingLinje(
        @Serializable(with = LocalDateSerializer::class)
        val fom: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val tom: LocalDate,
        val belop: Int,
        val tilskuddstype: Tilskuddstype,
    )

    /** Se Tiltaksforskriften § 7-5 */
    @Serializable
    enum class Tilskuddstype {
        SKOLEPENGER,
        STUDIEREISE,
        EKSAMENSAVGIFT,
        SEMESTERAVGIFT,
        INTEGRERT_BOTILBUD,
    }
}
