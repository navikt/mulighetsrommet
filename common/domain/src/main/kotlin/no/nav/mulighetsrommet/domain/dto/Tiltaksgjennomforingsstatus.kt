package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class Avslutningsstatus {
    AVLYST,
    AVBRUTT,
    AVSLUTTET,
    IKKE_AVSLUTTET;

    companion object {
        fun fromArenaStatus(arenaStatus: String): Avslutningsstatus {
            return when (arenaStatus) {
                "AVLYST" -> AVLYST
                "AVBRUTT" -> AVBRUTT
                "AVSLUTT" -> AVSLUTTET
                else -> IKKE_AVSLUTTET
            }
        }
    }
}

@Serializable
enum class Tiltaksgjennomforingsstatus {
    GJENNOMFORES,
    AVBRUTT,
    AVLYST,
    AVSLUTTET,
    APENT_FOR_INNSOK;

    companion object {
        fun fromDbo(
            startDato: LocalDate,
            sluttDato: LocalDate?,
            avslutningsStatus: Avslutningsstatus
        ): Tiltaksgjennomforingsstatus {
            return when {
                avslutningsStatus == Avslutningsstatus.AVLYST -> AVLYST
                avslutningsStatus == Avslutningsstatus.AVBRUTT -> AVBRUTT
                avslutningsStatus == Avslutningsstatus.AVSLUTTET -> AVSLUTTET
                startDato > LocalDate.now() -> APENT_FOR_INNSOK
                sluttDato == null -> GJENNOMFORES
                sluttDato >= LocalDate.now() -> GJENNOMFORES
                else -> AVSLUTTET
            }
        }
    }
}

