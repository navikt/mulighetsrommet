package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.services.SsbNusService
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.koin.ktor.ext.inject

fun Route.nusDataRoutes() {
    val nusService: SsbNusService by inject()

    route("api/v1/internal/nus-data") {
        post {
            val request = call.receive<NusDataRequest>()
            call.respond(
                nusService.getNusData(
                    tiltakskode = request.tiltakskode,
                    version = request.version,
                ),
            )
        }
    }
}

@Serializable
data class NusDataRequest(
    val tiltakskode: Tiltakskode,
    val version: String,
)
