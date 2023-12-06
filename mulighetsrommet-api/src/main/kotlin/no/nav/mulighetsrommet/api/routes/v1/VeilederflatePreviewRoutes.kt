package no.nav.mulighetsrommet.api.routes.v1

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.serialization.NonEmptyListSerializer
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
        post("/tiltaksgjennomforinger") {
            val request = call.receive<GetRelevanteTiltaksgjennomforingerPreviewRequest>()

            val result = veilederflateService.hentTiltaksgjennomforinger(
                enheter = nonEmptyListOf(request.geografiskEnhet),
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
                request.brukersEnheter,
                SanityPerspective.PREVIEW_DRAFTS,
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
    @Serializable(with = NonEmptyListSerializer::class)
    val brukersEnheter: NonEmptyList<String>,
    val id: String,
)
