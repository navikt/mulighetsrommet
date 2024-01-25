package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.GJENNOMFORES
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus.PLANLAGT
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
    val status: Tiltaksgjennomforingsstatus,
    val enhet: String,
    val apentForInnsok: Boolean,
    val deltidsprosent: Double,
) {
    companion object {
        fun from(
            tiltaksgjennomforing: TiltaksgjennomforingAdminDto,
            arenaId: Int?,
            endretTidspunkt: LocalDateTime,
        ): ArenaMigreringTiltaksgjennomforingDto {
            // Alle fra arena har arena_ansvarlig_enhet og nav_region er required fra admin-flate
            requireNotNull(tiltaksgjennomforing.navRegion ?: tiltaksgjennomforing.arenaAnsvarligEnhet) {
                "navRegion or arenaAnsvarligEnhet was null! Should not be possible!"
            }

            val status = when (tiltaksgjennomforing.status) {
                PLANLAGT, GJENNOMFORES -> GJENNOMFORES
                else -> tiltaksgjennomforing.status
            }

            return ArenaMigreringTiltaksgjennomforingDto(
                id = tiltaksgjennomforing.id,
                tiltakskode = tiltaksgjennomforing.tiltakstype.arenaKode,
                startDato = tiltaksgjennomforing.startDato,
                sluttDato = tiltaksgjennomforing.sluttDato,
                opprettetTidspunkt = tiltaksgjennomforing.createdAt,
                endretTidspunkt = endretTidspunkt,
                navn = tiltaksgjennomforing.navn,
                orgnummer = tiltaksgjennomforing.arrangor.organisasjonsnummer,
                antallPlasser = tiltaksgjennomforing.antallPlasser,
                status = status,
                arenaId = arenaId,
                enhet = tiltaksgjennomforing.arenaAnsvarligEnhet?.enhetsnummer
                    ?: tiltaksgjennomforing.navRegion?.enhetsnummer
                    ?: throw IllegalStateException("Unreachable pga require clause over"),
                apentForInnsok = tiltaksgjennomforing.apentForInnsok,
                deltidsprosent = tiltaksgjennomforing.deltidsprosent,
            )
        }
    }
}
