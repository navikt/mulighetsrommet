package no.nav.mulighetsrommet.domain.dto

import java.time.LocalDate
import java.time.LocalDateTime

enum class Avtalestatus {
    Aktiv,
    Avsluttet,
    Avbrutt,
    ;

    companion object {
        fun fromDbo(
            dagensDato: LocalDate,
            sluttDato: LocalDate?,
            avbruttTidspunkt: LocalDateTime?,
        ): Avtalestatus = when {
            avbruttTidspunkt != null -> Avbrutt
            sluttDato != null && dagensDato.isAfter(sluttDato) -> Avsluttet
            else -> Aktiv
        }
    }
}
