package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.domain.dto.JoyrideType
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideRequest
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.repositories.VeilederJoyrideRepository
import org.koin.ktor.ext.inject

fun Route.veilederJoyrideRoutes() {
    val veilederJoyrideRepository: VeilederJoyrideRepository by inject()

    route("/api/v1/intern/joyride") {
        post("lagre") {
            val request = call.receive<VeilederJoyrideRequest>()
            veilederJoyrideRepository.upsert(
                VeilederJoyrideDto(
                    navIdent = getNavIdent(),
                    fullfort = request.fullfort,
                    type = request.joyrideType,
                ),
            )
            call.respondText("ok")
        }

        get("{type}/har-fullfort") {
            val type = call.parameters.getOrFail("type")
            call.respond(
                veilederJoyrideRepository.harFullfortJoyride(
                    navIdent = getNavIdent(),
                    type = JoyrideType.valueOf(type),
                ),
            )
        }
    }
}
