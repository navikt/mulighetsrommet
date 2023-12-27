package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Serializable
data class ArenaMigreringTiltaksgjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val arenaId: Int?,
    val tiltakskode: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetTidspunkt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endretTidspunkt: LocalDateTime,
    val navn: String,
    val orgnummer: String,
    val antallPlasser: Int?,
    val status: Tiltaksgjennomforingsstatus,
    val enhet: String,
    @Serializable(with = LocalDateSerializer::class)
    val fremmoteDato: LocalDate?,
    @Serializable(with = LocalTimeSerializer::class)
    val fremmoteTid: LocalTime?,
    val fremmoteSted: String?,
    val apentForInnsok: Boolean,
) {
    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingAdminDto, arenaId: Int?, endretTidspunkt: LocalDateTime) =
            ArenaMigreringTiltaksgjennomforingDto(
                id = tiltaksgjennomforing.id,
                tiltakskode = tiltaksgjennomforing.tiltakstype.arenaKode,
                startDato = tiltaksgjennomforing.startDato,
                sluttDato = tiltaksgjennomforing.sluttDato,
                opprettetTidspunkt = tiltaksgjennomforing.createdAt,
                endretTidspunkt = endretTidspunkt,
                navn = tiltaksgjennomforing.navn,
                orgnummer = tiltaksgjennomforing.arrangor.organisasjonsnummer,
                antallPlasser = tiltaksgjennomforing.antallPlasser,
                status = tiltaksgjennomforing.status,
                arenaId = arenaId,
                // TODO: Hvilket enhet? Trenger vi nytt input felt?
                enhet = tiltaksgjennomforing.arenaAnsvarligEnhet?.enhetsnummer ?: "todo",
                fremmoteDato = tiltaksgjennomforing.fremmoteTidspunkt?.toLocalDate(),
                fremmoteTid = tiltaksgjennomforing.fremmoteTidspunkt?.toLocalTime()?.let {
                    if (it == LocalTime.of(0, 0)) {
                        null
                    } else {
                        it
                    }
                },
                fremmoteSted = tiltaksgjennomforing.fremmoteSted,
                apentForInnsok = tiltaksgjennomforing.apentForInnsok,
            )
    }
}
