package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus

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
}
