package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
            val enhetsnummer = gjennomforing.arena?.ansvarligNavEnhet
                ?: (gjennomforing as? GjennomforingAvtale)?.kontorstruktur?.firstOrNull()?.region?.enhetsnummer?.value
                ?: error("navRegion or arenaAnsvarligEnhet was null! Should not be possible!")

            val arenaStatus = when (gjennomforing.status.type) {
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
                opprettetTidspunkt = gjennomforing.opprettetTidspunkt.tilNorskLocalDateTime(),
                endretTidspunkt = gjennomforing.oppdatertTidspunkt.tilNorskLocalDateTime(),
                navn = gjennomforing.navn,
                orgnummer = gjennomforing.arrangor.organisasjonsnummer.value,
                antallPlasser = gjennomforing.antallPlasser,
                status = arenaStatus,
                arenaId = arenaId,
                enhet = enhetsnummer,
                apentForInnsok = when (gjennomforing) {
                    is GjennomforingAvtale -> gjennomforing.apentForPamelding
                    is GjennomforingEnkeltplass, is GjennomforingArena -> false
                },
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
