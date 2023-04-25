package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import java.time.LocalDate

@Serializable
enum class Tiltaksgjennomforingsstatus {
    GJENNOMFORES,
    AVBRUTT,
    AVLYST,
    AVSLUTTET,
    APENT_FOR_INNSOK,
    ;

    companion object {
        fun fromDbo(
            dagensDato: LocalDate,
            startDato: LocalDate,
            sluttDato: LocalDate?,
            avslutningsStatus: Avslutningsstatus,
        ): Tiltaksgjennomforingsstatus {
            return when {
                avslutningsStatus == Avslutningsstatus.AVLYST -> AVLYST
                avslutningsStatus == Avslutningsstatus.AVBRUTT -> AVBRUTT
                avslutningsStatus == Avslutningsstatus.AVSLUTTET -> AVSLUTTET
                startDato > dagensDato -> APENT_FOR_INNSOK
                sluttDato == null || sluttDato >= dagensDato -> GJENNOMFORES
                else -> AVSLUTTET
            }
        }
    }
}
