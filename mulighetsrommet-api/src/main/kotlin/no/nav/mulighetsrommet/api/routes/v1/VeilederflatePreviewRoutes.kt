package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.toNonEmptyListOrNull
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltaksgjennomforing
import no.nav.mulighetsrommet.api.services.VeilederflateService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.veilederflatePreviewRoutes() {
    val veilederflateService: VeilederflateService by inject()

    route("/api/v1/internal/veileder/preview") {
        post("/tiltaksgjennomforinger") {
            val request = call.receive<GetTiltaksgjennomforingerRequest>()
            val enheter = request.enheter.toNonEmptyListOrNull()
                ?: return@post call.respond(emptyList<VeilederflateTiltaksgjennomforing>())

            val result = veilederflateService.hentTiltaksgjennomforinger(
                enheter = enheter,
                innsatsgruppe = request.innsatsgruppe,
                tiltakstypeIds = request.tiltakstypeIds,
                search = request.search,
                apentForInnsok = request.apentForInnsok,
            )

            call.respond(result)
        }
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
