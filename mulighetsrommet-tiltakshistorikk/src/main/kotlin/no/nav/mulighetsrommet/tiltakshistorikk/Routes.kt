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
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkRequest
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
                    .getKometHistorikk(request.identer)

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

fun ArenaDeltakerDbo.toTiltakshistorikkDto() =
    TiltakshistorikkDto(
        id = this.id,
        startDato = this.startDato,
        sluttDato = this.sluttDato,
        status = this.status.toDeltakerstatus(),
        tiltaksnavn = this.beskrivelse,
        arenaTiltakskode = this.arenaTiltakskode,
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
