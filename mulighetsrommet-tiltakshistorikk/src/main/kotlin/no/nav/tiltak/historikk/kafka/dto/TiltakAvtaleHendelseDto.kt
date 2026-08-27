package no.nav.tiltak.historikk.kafka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class TiltakAvtaleHendelseDto(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val deltakerFnr: NorskIdent,
    val bedriftNr: String,
    val tiltakstype: Tiltakstype,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val avtaleStatus: Status,
    val stillingprosent: Float?,
    val antallDagerPerUke: Float?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @Serializable(with = InstantSerializer::class)
    val sistEndret: Instant,
) {
    @Serializable
    enum class Tiltakstype {
        ARBEIDSTRENING,
        MIDLERTIDIG_LONNSTILSKUDD,
        VARIG_LONNSTILSKUDD,
        MENTOR,
        INKLUDERINGSTILSKUDD,
        SOMMERJOBB,
        VTAO,
        FIREARIG_LONNSTILSKUDD,
    }

    @Serializable
    enum class Status {
        @SerialName("PÅBEGYNT")
        PAABEGYNT,

        @SerialName("MANGLER_GODKJENNING")
        MANGLER_GODKJENNING,

        @SerialName("KLAR_FOR_OPPSTART")
        KLAR_FOR_OPPSTART,

        @SerialName("GJENNOMFØRES")
        GJENNOMFORES,

        @SerialName("AVSLUTTET")
        AVSLUTTET,

        @SerialName("AVBRUTT")
        AVBRUTT,

        @SerialName("ANNULLERT")
        ANNULLERT,
    }
}
