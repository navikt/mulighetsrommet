package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.api.utils.getPaginationParams
import no.nav.mulighetsrommet.api.utils.getTiltakstypeFilter
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tiltakstypeRoutes() {
    val tiltakstypeService: TiltakstypeService by inject()
    val veilederflateService: VeilederflateService by inject()

    route("/api/v1/internal/tiltakstyper") {
        get {
            val filter = getTiltakstypeFilter()
            val paginationParams = getPaginationParams()

            call.respond(
                tiltakstypeService.getWithFilter(
                    filter,
                    paginationParams,
                ),
            )
        }
        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            call.respond(tiltakstype)
        }

        get("{id}/sanity") {
            val id = call.parameters.getOrFail<UUID>("id")
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            val veilederflateTiltakstype: VeilederflateTiltakstype = veilederflateService.hentTiltakstyper()
                .find { it.sanityId != null && UUID.fromString(it.sanityId) == tiltakstype.sanityId }
                ?: return@get call.respondText(
                    "Det finnes ikke noe faneinnhold for tiltakstype med id $id",
                    status = HttpStatusCode.NotFound,
                )

            call.respond(veilederflateTiltakstype)
        }

        get("{id}/nokkeltall") {
            val id = call.parameters.getOrFail<UUID>("id")
            val nokkeltall = tiltakstypeService.getNokkeltallForTiltakstype(id)

            call.respond(nokkeltall)
        }
    }
}
