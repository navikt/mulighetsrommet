package no.nav.mulighetsrommet.domain.dbo

import java.time.LocalDate
import java.time.LocalDateTime

enum class Avslutningsstatus {
    AVLYST,
    AVBRUTT,
    AVSLUTTET,
    IKKE_AVSLUTTET,
    ;

    companion object {
        fun fromArenastatus(arenaStatus: String): Avslutningsstatus {
            return when (arenaStatus) {
                "AVLYST" -> AVLYST
                "AVBRUTT" -> AVBRUTT
                "AVSLUTT" -> AVSLUTTET
                else -> IKKE_AVSLUTTET
            }
        }

        fun fromAvbruttTidspunkt(
            dagensDato: LocalDate,
            startDato: LocalDate,
            sluttDato: LocalDate?,
            avbruttTidspunkt: LocalDateTime?,
        ): Avslutningsstatus {
            return when {
                avbruttTidspunkt != null && avbruttTidspunkt.toLocalDate().isBefore(startDato) -> AVLYST
                avbruttTidspunkt != null && !avbruttTidspunkt.toLocalDate().isBefore(startDato) -> AVBRUTT
                sluttDato != null && dagensDato.isAfter(sluttDato) -> AVSLUTTET
                else -> IKKE_AVSLUTTET
            }
        }
    }
}
