package no.nav.mulighetsrommet.domain.dbo

import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus

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

        fun fromTiltaksgjennomforingStatus(status: TiltaksgjennomforingStatus): Avslutningsstatus = when (status) {
            TiltaksgjennomforingStatus.PLANLAGT, TiltaksgjennomforingStatus.GJENNOMFORES -> IKKE_AVSLUTTET
            TiltaksgjennomforingStatus.AVLYST -> AVLYST
            TiltaksgjennomforingStatus.AVBRUTT -> AVBRUTT
            TiltaksgjennomforingStatus.AVSLUTTET -> AVSLUTTET
        }
    }
}
