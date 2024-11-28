package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
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
    val status: ArenaTiltaksgjennomforingStatus,
    val enhet: String,
    val apentForInnsok: Boolean,
    val deltidsprosent: Double,
) {
    companion object {
        fun from(
            tiltaksgjennomforing: TiltaksgjennomforingDto,
            arenaId: Int?,
            endretTidspunkt: LocalDateTime,
        ): ArenaMigreringTiltaksgjennomforingDto {
            val enhetsnummer = if (tiltaksgjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
                tiltaksgjennomforing.arenaAnsvarligEnhet?.enhetsnummer
            } else {
                tiltaksgjennomforing.navRegion?.enhetsnummer
            }
            requireNotNull(enhetsnummer) {
                "navRegion or arenaAnsvarligEnhet was null! Should not be possible!"
            }

            val status = when (tiltaksgjennomforing.status.status) {
                TiltaksgjennomforingStatus.GJENNOMFORES -> ArenaTiltaksgjennomforingStatus.GJENNOMFORES
                TiltaksgjennomforingStatus.AVSLUTTET -> ArenaTiltaksgjennomforingStatus.AVSLUTTET
                TiltaksgjennomforingStatus.AVBRUTT -> ArenaTiltaksgjennomforingStatus.AVBRUTT
                TiltaksgjennomforingStatus.AVLYST -> ArenaTiltaksgjennomforingStatus.AVLYST
            }

            return ArenaMigreringTiltaksgjennomforingDto(
                id = tiltaksgjennomforing.id,
                tiltakskode = tiltaksgjennomforing.tiltakstype.tiltakskode.toArenaKode(),
                startDato = tiltaksgjennomforing.startDato,
                sluttDato = tiltaksgjennomforing.sluttDato,
                opprettetTidspunkt = tiltaksgjennomforing.createdAt,
                endretTidspunkt = endretTidspunkt,
                navn = tiltaksgjennomforing.navn,
                orgnummer = tiltaksgjennomforing.arrangor.organisasjonsnummer.value,
                antallPlasser = tiltaksgjennomforing.antallPlasser,
                status = status,
                arenaId = arenaId,
                enhet = enhetsnummer,
                apentForInnsok = tiltaksgjennomforing.apentForPamelding,
                deltidsprosent = tiltaksgjennomforing.deltidsprosent,
            )
        }
    }
}

enum class ArenaTiltaksgjennomforingStatus {
    GJENNOMFORES,
    AVSLUTTET,
    AVBRUTT,
    AVLYST,
}
