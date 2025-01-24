package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.AvbruttAarsakSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

enum class GjennomforingStatus {
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT,
    AVLYST,
}

@Serializable
data class GjennomforingStatusDto(
    val status: GjennomforingStatus,
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
