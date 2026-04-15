package no.nav.mulighetsrommet.api.utbetaling.service

import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.Instant
import java.time.ZoneId

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
            .plusDays(37)
            .atStartOfDay()
            .atZone(ZoneId.of("Europe/Oslo"))
            .toInstant()

        else -> null
    }
}

val tidligstTidspunktForUtbetalingDev = TidligstTidspunktForUtbetalingCalculator { tiltakskode, periode ->
    when (tiltakskode) {
        Tiltakskode.OPPFOLGING,
        Tiltakskode.AVKLARING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
        -> periode.getLastInclusiveDate()
            .plusDays(37)
            .atStartOfDay()
            .atZone(ZoneId.of("Europe/Oslo"))
            .toInstant()

        else -> null
    }
}
