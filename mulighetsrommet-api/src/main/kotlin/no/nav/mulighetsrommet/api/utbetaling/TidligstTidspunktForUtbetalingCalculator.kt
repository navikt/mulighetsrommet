package no.nav.mulighetsrommet.api.utbetaling

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun interface TidligstTidspunktForUtbetalingCalculator {
    fun calculate(tiltakskode: Tiltakskode, periode: Periode): Instant?
}

val tidligstTidspunktForUtbetalingProd = TidligstTidspunktForUtbetalingCalculator { tiltakskode, periode ->
    when (tiltakskode) {
        Tiltakskode.OPPFOLGING,
        Tiltakskode.AVKLARING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        -> periode.getLastInclusiveDate()
            .withDayOfMonth(7)
            .plusMonths(1)
            .plusDays(30)
            .atStartOfDay()
            .atZone(ZoneId.of("Europe/Oslo"))
            .toInstant()

        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        -> null
    }
}

val tidligstTidspunktForUtbetalingDev = TidligstTidspunktForUtbetalingCalculator { tiltakskode, _ ->
    when (tiltakskode) {
        Tiltakskode.OPPFOLGING,
        Tiltakskode.AVKLARING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        -> Instant.now().plus(5, ChronoUnit.MINUTES)

        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_UTDANNING,
        Tiltakskode.JOBBKLUBB,
        Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
        Tiltakskode.STUDIESPESIALISERING,
        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING,
        -> null
    }
}
