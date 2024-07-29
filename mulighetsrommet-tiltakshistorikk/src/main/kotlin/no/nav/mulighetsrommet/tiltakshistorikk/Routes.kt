package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.async
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkRequest
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkResponse
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import java.util.*

fun Route.tiltakshistorikkRoutes(deltakerRepository: DeltakerRepository) {
    authenticate {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkRequest>()

                val arenaDeltakelser = async { deltakerRepository.getArenaHistorikk(request.identer, request.maxAgeYears) }
                val gruppetiltakDeltakelser = async { deltakerRepository.getKometHistorikk(request.identer, request.maxAgeYears) }
                val tiltakshistorikk = arenaDeltakelser.await() + gruppetiltakDeltakelser.await()

                val historikk = tiltakshistorikk.sortedWith(compareBy(nullsLast()) { it.startDato })
                call.respond(TiltakshistorikkResponse(historikk = historikk))
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
