package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.serializers.AvbruttAarsakSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class TiltaksgjennomforingStatus {
    PLANLAGT,
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT,
    AVLYST,
}

@Serializable
data class TiltaksgjennomforingStatusDto(
    val status: TiltaksgjennomforingStatus,
    val avbrutt: AvbruttDto?,
) {
    fun toAvslutningsstatus(): Avslutningsstatus =
        when (status) {
            TiltaksgjennomforingStatus.PLANLAGT, TiltaksgjennomforingStatus.GJENNOMFORES -> Avslutningsstatus.IKKE_AVSLUTTET
            TiltaksgjennomforingStatus.AVSLUTTET -> Avslutningsstatus.AVSLUTTET
            TiltaksgjennomforingStatus.AVBRUTT -> Avslutningsstatus.AVBRUTT
            TiltaksgjennomforingStatus.AVLYST -> Avslutningsstatus.AVLYST
        }
}

@Serializable
data class AvbruttDto(
    @Serializable(with = LocalDateTimeSerializer::class)
    val tidspunkt: LocalDateTime,
    @Serializable(with = AvbruttAarsakSerializer::class)
    val aarsak: AvbruttAarsak,
    val beskrivelse: String,
)
