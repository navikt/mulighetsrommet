package no.nav.mulighetsrommet.model

import java.time.LocalDateTime

sealed class AvtaleStatus {
    abstract val enum: Enum

    data object AKTIV : AvtaleStatus() {
        override val enum = Enum.AKTIV
    }
    data object AVSLUTTET : AvtaleStatus() {
        override val enum = Enum.AVSLUTTET
    }
    data object UTKAST : AvtaleStatus() {
        override val enum = Enum.UTKAST
    }
    data class AVBRUTT(val tidspunkt: LocalDateTime, val aarsak: AvbruttAarsak) : AvtaleStatus() {
        override val enum = Enum.AVBRUTT
    }

    companion object {
        fun fromString(name: String, tidspunkt: LocalDateTime?, aarsak: AvbruttAarsak?): AvtaleStatus = when (Enum.valueOf(name)) {
            Enum.AKTIV -> AKTIV
            Enum.AVSLUTTET -> AVSLUTTET
            Enum.UTKAST -> UTKAST
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
        UTKAST,
    }
}
