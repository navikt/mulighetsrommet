package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.ArenaMigreringTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.dto.JaNeiStatus
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArenaMigreringTiltaksgjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val ID: UUID,
    val TILTAKSKODE: String,
    val REG_DATO: String,
    val DATO_FRA: String,
    val DATO_TIL: String?,
    val LOKALTNAVN: String,
    val ARBGIV_ID_ARRANGOR: Int?,
    val STATUS_TREVERDIKODE_INNSOKNING: JaNeiStatus?,
    val ANTALL_DELTAKERE: Int,
    val TILTAKSTATUSKODE: String,
) {
    companion object {
        fun from(arenaMigreringTiltaksgjennomforingDbo: ArenaMigreringTiltaksgjennomforingDbo) =
            ArenaMigreringTiltaksgjennomforingDto(
                ID = arenaMigreringTiltaksgjennomforingDbo.id,
                TILTAKSKODE = arenaMigreringTiltaksgjennomforingDbo.tiltakskode,
                REG_DATO = ArenaTimestampFormatter.format(arenaMigreringTiltaksgjennomforingDbo.createdAt),
                DATO_FRA = ArenaTimestampFormatter.format(arenaMigreringTiltaksgjennomforingDbo.startDato.atStartOfDay()),
                DATO_TIL = arenaMigreringTiltaksgjennomforingDbo.sluttDato?.let { ArenaTimestampFormatter.format(it.atStartOfDay()) },
                LOKALTNAVN = arenaMigreringTiltaksgjennomforingDbo.navn,
                ARBGIV_ID_ARRANGOR = null, // amt-arena-ord eksponerer ikke endepunkt for henting av denne
                // baser på virksomhetsnummer...
                STATUS_TREVERDIKODE_INNSOKNING = if (arenaMigreringTiltaksgjennomforingDbo.tilgjengelighet ==
                    TiltaksgjennomforingTilgjengelighetsstatus.LEDIG
                ) {
                    JaNeiStatus.Ja
                } else {
                    JaNeiStatus.Nei
                },
                ANTALL_DELTAKERE = arenaMigreringTiltaksgjennomforingDbo.antallPlasser,
                TILTAKSTATUSKODE = arenaMigreringTiltaksgjennomforingDbo.avslutningsstatus.toArenastatus(),
            )
    }
}
