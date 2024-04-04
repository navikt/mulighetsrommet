package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
enum class Tiltaksgjennomforingsstatus {
    PLANLAGT,
    GJENNOMFORES,
    AVBRUTT,
    AVLYST,
    AVSLUTTET,
    ;

    fun toAvslutningsstatus(): Avslutningsstatus =
        when (this) {
            PLANLAGT, GJENNOMFORES -> Avslutningsstatus.IKKE_AVSLUTTET
            AVLYST -> Avslutningsstatus.AVLYST
            AVBRUTT -> Avslutningsstatus.AVBRUTT
            AVSLUTTET -> Avslutningsstatus.AVSLUTTET
        }

    companion object {
        fun fromDbo(
            dagensDato: LocalDate,
            startDato: LocalDate,
            sluttDato: LocalDate?,
            avbruttTidspunkt: LocalDateTime?,
        ): Tiltaksgjennomforingsstatus =
            when {
                avbruttTidspunkt != null && avbruttTidspunkt.toLocalDate().isBefore(startDato) -> AVLYST
                avbruttTidspunkt != null && !avbruttTidspunkt.toLocalDate().isBefore(startDato) -> AVBRUTT
                sluttDato != null && !dagensDato.isBefore(sluttDato) -> AVSLUTTET
                !startDato.isAfter(dagensDato) -> GJENNOMFORES
                else -> PLANLAGT
            }
    }
}
