package no.nav.mulighetsrommet.domain.dto

import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import java.time.LocalDateTime

sealed class TiltaksgjennomforingStatus {
    enum class Enum {
        PLANLAGT,
        GJENNOMFORES,
        AVSLUTTET,
        AVBRUTT,
        AVLYST,
    }

    abstract val enum: Enum

    object PLANLAGT : TiltaksgjennomforingStatus() {
        override val enum = Enum.PLANLAGT
    }
    object GJENNOMFORES : TiltaksgjennomforingStatus() {
        override val enum = Enum.GJENNOMFORES
    }
    object AVSLUTTET : TiltaksgjennomforingStatus() {
        override val enum = Enum.AVSLUTTET
    }
    data class AVBRUTT(val tidspunkt: LocalDateTime, val aarsak: AvbruttAarsak) : TiltaksgjennomforingStatus() {
        override val enum = Enum.AVBRUTT
    }
    data class AVLYST(val tidspunkt: LocalDateTime, val aarsak: AvbruttAarsak) : TiltaksgjennomforingStatus() {
        override val enum = Enum.AVLYST
    }

    fun toAvslutningsstatus(): Avslutningsstatus =
        when (this) {
            is PLANLAGT, is GJENNOMFORES -> Avslutningsstatus.IKKE_AVSLUTTET
            is AVLYST -> Avslutningsstatus.AVLYST
            is AVBRUTT -> Avslutningsstatus.AVBRUTT
            is AVSLUTTET -> Avslutningsstatus.AVSLUTTET
        }

    companion object {
        fun fromString(name: String, tidspunkt: LocalDateTime?, aarsak: AvbruttAarsak?): TiltaksgjennomforingStatus =
            when (Enum.valueOf(name)) {
                Enum.PLANLAGT -> PLANLAGT
                Enum.GJENNOMFORES -> GJENNOMFORES
                Enum.AVSLUTTET -> AVSLUTTET
                Enum.AVBRUTT -> {
                    requireNotNull(tidspunkt)
                    requireNotNull(aarsak)
                    AVBRUTT(tidspunkt, aarsak)
                }
                Enum.AVLYST -> {
                    requireNotNull(tidspunkt)
                    requireNotNull(aarsak)
                    AVLYST(tidspunkt, aarsak)
                }
            }
    }
}
