package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.AvbruttAarsakSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class TiltaksgjennomforingStatus {
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT,
    AVLYST,
}

@Serializable
data class TiltaksgjennomforingStatusDto(
    val status: TiltaksgjennomforingStatus,
    val avbrutt: AvbruttDto?,
)

@Serializable
data class AvbruttDto(
    @Serializable(with = LocalDateTimeSerializer::class)
    val tidspunkt: LocalDateTime,
    @Serializable(with = AvbruttAarsakSerializer::class)
    val aarsak: AvbruttAarsak,
    val beskrivelse: String,
)
