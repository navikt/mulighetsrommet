package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArenaMigreringTiltaksgjennomforingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val arenaId: Int?,
    val tiltakskode: String,
    val startDato: String,
    val sluttDato: String?,
    val navn: String,
    val orgnummer: String,
    val antallPlasser: Int?,
    val status: Tiltaksgjennomforingsstatus,
    val enhet: String,
) {
    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingAdminDto, arenaId: Int?) =
            ArenaMigreringTiltaksgjennomforingDto(
                id = tiltaksgjennomforing.id,
                tiltakskode = tiltaksgjennomforing.tiltakstype.arenaKode,
                startDato = ArenaTimestampFormatter.format(tiltaksgjennomforing.startDato.atStartOfDay()),
                sluttDato = tiltaksgjennomforing.sluttDato?.let { ArenaTimestampFormatter.format(it.atStartOfDay()) },
                navn = tiltaksgjennomforing.navn,
                orgnummer = tiltaksgjennomforing.arrangor.organisasjonsnummer,
                antallPlasser = tiltaksgjennomforing.antallPlasser,
                status = tiltaksgjennomforing.status,
                arenaId = arenaId,
                enhet = tiltaksgjennomforing.arenaAnsvarligEnhet ?: "todo", // TODO: Hvilket enhet? Trenger vi nytt input felt?
            )
    }
}
