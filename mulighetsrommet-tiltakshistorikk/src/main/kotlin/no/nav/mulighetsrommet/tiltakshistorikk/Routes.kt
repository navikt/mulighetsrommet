package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkRequest
import no.nav.mulighetsrommet.tiltakshistorikk.repositories.DeltakerRepository
import java.util.*

fun Route.tiltakshistorikkRoutes(
    deltakerRepository: DeltakerRepository,
    service: TiltakshistorikkService,
) {
    authenticate {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkRequest>()

                val response = service.getTiltakshistorikk(request)

                call.respond(response)
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
