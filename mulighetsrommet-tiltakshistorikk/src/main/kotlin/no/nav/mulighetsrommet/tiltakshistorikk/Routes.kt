package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import java.util.*

fun Route.tiltakshistorikkRoutes(deltakerRepository: DeltakerRepository) {
    authenticate {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkRequest>()
                val arenaHistorikk = deltakerRepository
                    .getArenaDeltakelser(request.identer)
                    .map { it.toTiltakshistorikkDto() }
                val kometHistorikk = deltakerRepository
                    .getKometDeltakelser(request.identer)
                    .map { it.toTiltakshistorikkDto() }

                call.respond(arenaHistorikk + kometHistorikk)
            }
        }

        route("/api/v1/intern/arena") {
            put("/deltaker") {
                val dbo = call.receive<ArenaDeltakerDbo>()

                deltakerRepository.upsertArenaDeltaker(dbo)

                call.respond(HttpStatusCode.OK)
            }

            delete("/deltaker/{id}") {
                val id: UUID by call.parameters

                deltakerRepository.deleteArenaDeltaker(id)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

data class TiltakshistorikkRequest(
    val identer: List<NorskIdent>,
)

fun ArenaDeltakerDbo.toTiltakshistorikkDto() =
    TiltakshistorikkDto(
        id = this.id,
        gjennomforingId = null,
        startDato = this.startDato,
        sluttDato = this.sluttDato,
        status = this.status.toDeltakerstatus(),
        tiltaksnavn = this.beskrivelse,
        tiltakstype = this.arenaTiltakskode,
        arrangorOrganisasjonsnummer = this.arrangorOrganisasjonsnummer,
    )

fun ArenaDeltakerStatus.toDeltakerstatus() =
    when (this) {
        ArenaDeltakerStatus.AVSLAG,
        ArenaDeltakerStatus.IKKE_AKTUELL,
        ArenaDeltakerStatus.TAKKET_NEI_TIL_TILBUD,
        -> Deltakerstatus.IKKE_AKTUELL

        ArenaDeltakerStatus.TILBUD,
        ArenaDeltakerStatus.TAKKET_JA_TIL_TILBUD,
        ArenaDeltakerStatus.INFORMASJONSMOTE,
        ArenaDeltakerStatus.AKTUELL,
        ArenaDeltakerStatus.VENTELISTE,
        -> Deltakerstatus.VENTER

        ArenaDeltakerStatus.GJENNOMFORES -> Deltakerstatus.DELTAR

        ArenaDeltakerStatus.DELTAKELSE_AVBRUTT,
        ArenaDeltakerStatus.GJENNOMFORING_AVBRUTT,
        ArenaDeltakerStatus.GJENNOMFORING_AVLYST,
        ArenaDeltakerStatus.FULLFORT,
        ArenaDeltakerStatus.IKKE_MOTT,
        -> Deltakerstatus.AVSLUTTET
    }

fun AmtDeltakerV1Dto.toTiltakshistorikkDto() =
    TiltakshistorikkDto(
        id = this.id,
        gjennomforingId = this.gjennomforingId,
        startDato = this.startDato?.atStartOfDay(),
        sluttDato = this.sluttDato?.atStartOfDay(),
        status = this.status.toDeltakerstatus(),
        tiltaksnavn = null,
        tiltakstype = null,
        arrangorOrganisasjonsnummer = null,
    )

fun AmtDeltakerStatus.toDeltakerstatus() =
    when (this.type) {
        AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING -> Deltakerstatus.PABEGYNT_REGISTRERING

        AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING, // TODO: Skal denne her? Dette er vel før påbegynt registrering egentlig?
        AmtDeltakerStatus.Type.VURDERES, // TODO: Skal denne her? Dette er vel før påbegynt registrering egentlig?
        AmtDeltakerStatus.Type.SOKT_INN,
        AmtDeltakerStatus.Type.VENTELISTE,
        AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
        -> Deltakerstatus.VENTER

        AmtDeltakerStatus.Type.AVBRUTT_UTKAST,
        AmtDeltakerStatus.Type.IKKE_AKTUELL,
        AmtDeltakerStatus.Type.FEILREGISTRERT,
        -> Deltakerstatus.IKKE_AKTUELL

        AmtDeltakerStatus.Type.DELTAR -> Deltakerstatus.DELTAR

        AmtDeltakerStatus.Type.HAR_SLUTTET,
        AmtDeltakerStatus.Type.AVBRUTT,
        AmtDeltakerStatus.Type.FULLFORT,
        -> Deltakerstatus.AVSLUTTET
    }
