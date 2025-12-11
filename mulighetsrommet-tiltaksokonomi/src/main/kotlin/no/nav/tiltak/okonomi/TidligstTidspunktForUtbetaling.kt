package no.nav.tiltak.okonomi

import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.okonomi.model.Bestilling
import no.nav.tiltak.okonomi.model.Faktura
import java.time.LocalDateTime

val tidligstTidspunktForUtbetalingProd: (Bestilling, Faktura) -> LocalDateTime? = { bestilling, faktura ->
    when (bestilling.tiltakskode) {
        Tiltakskode.OPPFOLGING,
        Tiltakskode.AVKLARING,
        Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        -> faktura.periode.getLastInclusiveDate()
            .withDayOfMonth(7)
            .plusMonths(1)
            .plusDays(30)
            .atStartOfDay()

        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
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

val tidligstTidspunktForUtbetalingDev: (Bestilling, Faktura) -> LocalDateTime? = { _, faktura ->
    faktura.besluttetTidspunkt.plusMinutes(1)
}
