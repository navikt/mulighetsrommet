package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class TiltaksgjennomforingV1Dto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val virksomhetsnummer: String,
    val oppstart: GjennomforingOppstartstype,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorFraOgMedDato: LocalDate?,
    val apentForPamelding: Boolean,
    val antallPlasser: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val oppdatertTidspunkt: LocalDateTime,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
        val tiltakskode: Tiltakskode,
    )
}
