package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.services.VeilederflateService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.veilederflatePreviewRoutes() {
    val veilederflateService: VeilederflateService by inject()

    route("/api/v1/internal/veileder/preview") {
        post("/tiltaksgjennomforing") {
            val request = call.receive<GetTiltaksgjennomforingDetaljerPreviewRequest>()

            val id = UUID.fromString(request.id.replace("drafts.", ""))
            val result = veilederflateService.hentTiltaksgjennomforing(
                id,
                request.enheter,
                SanityPerspective.PREVIEW_DRAFTS,
            )

            call.respond(result)
        }
    }
}

@Serializable
data class GetTiltaksgjennomforingDetaljerPreviewRequest(
    val enheter: List<String>,
    val id: String,
)
