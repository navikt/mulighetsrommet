package no.nav.mulighetsrommet.domain.dto

import java.time.LocalDate

enum class Tiltakstypestatus {
    Aktiv,
    Planlagt,
    Avsluttet,
    ;

    companion object {
        fun resolveFromDates(
            now: LocalDate,
            startDato: LocalDate,
            sluttDato: LocalDate,
        ): Tiltakstypestatus = when {
            now < startDato -> Planlagt
            now <= sluttDato -> Aktiv
            else -> Avsluttet
        }
    }
}
