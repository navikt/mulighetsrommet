package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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
            gjennomforing: Gjennomforing,
            arenaId: Int?,
        ): ArenaMigreringTiltaksgjennomforingDto {
            val enhetsnummer = if (gjennomforing.opphav == ArenaMigrering.Opphav.ARENA) {
                gjennomforing.arenaAnsvarligEnhet?.enhetsnummer
            } else {
                gjennomforing.kontorstruktur.firstOrNull()?.region?.enhetsnummer?.value
            }
            requireNotNull(enhetsnummer) {
                "navRegion or arenaAnsvarligEnhet was null! Should not be possible!"
            }

            val status = when (gjennomforing.status.type) {
                GjennomforingStatusType.GJENNOMFORES -> ArenaTiltaksgjennomforingStatus.GJENNOMFORES
                GjennomforingStatusType.AVSLUTTET -> ArenaTiltaksgjennomforingStatus.AVSLUTTET
                GjennomforingStatusType.AVBRUTT -> ArenaTiltaksgjennomforingStatus.AVBRUTT
                GjennomforingStatusType.AVLYST -> ArenaTiltaksgjennomforingStatus.AVLYST
            }

            return ArenaMigreringTiltaksgjennomforingDto(
                id = gjennomforing.id,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode.arenakode,
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt,
                endretTidspunkt = gjennomforing.oppdatertTidspunkt,
                navn = gjennomforing.navn,
                orgnummer = gjennomforing.arrangor.organisasjonsnummer.value,
                antallPlasser = gjennomforing.antallPlasser,
                status = status,
                arenaId = arenaId,
                enhet = enhetsnummer,
                apentForInnsok = gjennomforing.apentForPamelding,
                deltidsprosent = gjennomforing.deltidsprosent,
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
