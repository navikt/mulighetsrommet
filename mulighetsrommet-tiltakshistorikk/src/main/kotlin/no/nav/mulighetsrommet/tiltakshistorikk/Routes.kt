package no.nav.mulighetsrommet.tiltakshistorikk

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import java.util.*

fun Route.tiltakshistorikkRoutes(deltakerRepository: DeltakerRepository) {
    authenticate {
        route("/api/v1/intern/arena") {
            put("/deltaker") {
                val dbo = call.receive<ArenaDeltakerDbo>()

                deltakerRepository.upsert(dbo)

                call.respond(HttpStatusCode.OK)
            }

            delete("/deltaker/{id}") {
                val id: UUID by call.parameters

                deltakerRepository.delete(id)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
