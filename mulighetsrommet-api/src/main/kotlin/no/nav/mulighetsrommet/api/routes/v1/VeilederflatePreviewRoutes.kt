package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.services.VeilederflateService
import org.koin.ktor.ext.inject

fun Route.veilederflatePreviewRoutes() {
    val veilederflateService: VeilederflateService by inject()

    route("/api/v1/internal/sanity") {
        post("/tiltaksgjennomforinger/preview") {
            val request = call.receive<GetRelevanteTiltaksgjennomforingerPreviewRequest>()
            val result = veilederflateService.hentPreviewTiltaksgjennomforinger(request)
            call.respond(result)
        }

        post("/tiltaksgjennomforing/preview/detaljer") {
            val request = call.receive<GetTiltaksgjennomforingDetaljerPreviewRequest>()
            val result = veilederflateService.hentPreviewTiltaksgjennomforing(
                request.id,
                request.brukersEnheter,
            )
            call.respond(result)
        }
    }
}

@Serializable
data class GetRelevanteTiltaksgjennomforingerPreviewRequest(
    val innsatsgruppe: String? = null,
    val tiltakstypeIds: List<String>? = null,
    val search: String? = null,
    val geografiskEnhet: String,
    val apentForInnsok: ApentForInnsok,
)

@Serializable
data class GetTiltaksgjennomforingDetaljerPreviewRequest(
    val brukersEnheter: List<String>,
    val id: String,
)
