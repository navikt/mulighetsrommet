package no.nav.mulighetsrommet.domain.dto

import java.time.LocalDateTime

sealed class AvtaleStatus {
    abstract val enum: Enum

    object AKTIV : AvtaleStatus() {
        override val enum = Enum.AKTIV
    }
    object AVSLUTTET : AvtaleStatus() {
        override val enum = Enum.AVSLUTTET
    }
    data class AVBRUTT(val tidspunkt: LocalDateTime, val aarsak: AvbruttAarsak) : AvtaleStatus() {
        override val enum = Enum.AVBRUTT
    }

    companion object {
        fun fromString(name: String, tidspunkt: LocalDateTime?, aarsak: AvbruttAarsak?): AvtaleStatus =
            when (Enum.valueOf(name)) {
                Enum.AKTIV -> AKTIV
                Enum.AVSLUTTET -> AVSLUTTET
                Enum.AVBRUTT -> {
                    requireNotNull(tidspunkt)
                    requireNotNull(aarsak)
                    AVBRUTT(tidspunkt, aarsak)
                }
            }
    }

    enum class Enum {
        AKTIV,
        AVSLUTTET,
        AVBRUTT,
    }
}
