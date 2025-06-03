package no.nav.mulighetsrommet.api.gjennomforing.mapper

import no.nav.mulighetsrommet.model.GjennomforingStatus
import java.time.LocalDate
import java.time.LocalDateTime

object GjennomforingStatusMapper {
    fun fromSluttDato(sluttDato: LocalDate?, today: LocalDate): GjennomforingStatus {
        return if (sluttDato == null || !sluttDato.isBefore(today)) {
            GjennomforingStatus.GJENNOMFORES
        } else {
            GjennomforingStatus.AVSLUTTET
        }
    }

    fun fromAvsluttetTidspunkt(
        startDato: LocalDate,
        sluttDato: LocalDate?,
        avsluttetTidspunkt: LocalDateTime,
    ): GjennomforingStatus {
        val tidspunktForStart = startDato.atStartOfDay()
        val tidspunktForSlutt = sluttDato?.plusDays(1)?.atStartOfDay()
        return if (avsluttetTidspunkt.isBefore(tidspunktForStart)) {
            GjennomforingStatus.AVLYST
        } else if (tidspunktForSlutt == null || avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            GjennomforingStatus.AVBRUTT
        } else {
            GjennomforingStatus.AVSLUTTET
        }
    }
}
