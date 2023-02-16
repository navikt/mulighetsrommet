package no.nav.mulighetsrommet.domain.dto

import java.time.LocalDate

enum class Avtalestatus {
    Planlagt,
    Aktiv,
    Avsluttet,
    Avbrutt;

    companion object {
        fun resolveFromDates(now: LocalDate, startDato: LocalDate, sluttDato: LocalDate): Avtalestatus = when {
            now < startDato -> Planlagt
            now <= sluttDato -> Aktiv
            else -> Avsluttet
        }
    }
}
