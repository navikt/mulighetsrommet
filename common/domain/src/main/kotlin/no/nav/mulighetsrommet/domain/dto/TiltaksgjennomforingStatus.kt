package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import java.time.LocalDate

@Serializable
enum class TiltaksgjennomforingStatus {
    PLANLAGT,
    GJENNOMFORES,
    AVBRUTT,
    AVLYST,
    AVSLUTTET,
    ;

    companion object {
        fun fromDbo(
            dagensDato: LocalDate,
            startDato: LocalDate,
            sluttDato: LocalDate?,
            avslutningsStatus: Avslutningsstatus,
        ): TiltaksgjennomforingStatus {
            return when {
                avslutningsStatus == Avslutningsstatus.AVLYST -> AVLYST
                avslutningsStatus == Avslutningsstatus.AVBRUTT -> AVBRUTT
                avslutningsStatus == Avslutningsstatus.AVSLUTTET -> AVSLUTTET
                startDato > dagensDato -> PLANLAGT
                sluttDato == null || sluttDato >= dagensDato -> GJENNOMFORES
                else -> AVSLUTTET
            }
        }
    }
}
