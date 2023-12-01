package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.domain.dto.HarFullfortJoyrideRequest
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideDto
import no.nav.mulighetsrommet.api.domain.dto.VeilederJoyrideRequest
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.VeilederJoyrideService
import org.koin.ktor.ext.inject

fun Route.veilederJoyrideRoutes() {
    val veilederJoyrideService: VeilederJoyrideService by inject()

    route("/api/v1/internal/joyride") {
        post("lagre") {
            val request = call.receive<VeilederJoyrideRequest>()
            call.respond(
                veilederJoyrideService.save(
                    VeilederJoyrideDto(
                        navident = getNavIdent(),
                        fullfort = request.fullfort,
                        type = request.type,
                    ),
                ),
            )
        }

        post("harFullfort") {
            val request = call.receive<HarFullfortJoyrideRequest>()
            call.respond(veilederJoyrideService.harFullfortJoyride(navident = getNavIdent(), type = request.type))
        }
    }
}
