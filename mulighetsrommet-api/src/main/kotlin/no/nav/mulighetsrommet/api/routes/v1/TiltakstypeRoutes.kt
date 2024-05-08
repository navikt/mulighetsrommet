package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.util.pipeline.*
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltakstype
import no.nav.mulighetsrommet.api.routes.v1.parameters.getPaginationParams
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskode.Companion.toArenaKode
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tiltakstypeRoutes(migrerteTiltak: List<Tiltakskode>) {
    val tiltakstypeService: TiltakstypeService by inject()
    val veilederflateService: VeilederflateService by inject()

    route("/api/v1/internal/tiltakstyper") {
        get {
            val filter = getTiltakstypeFilter()
            val pagination = getPaginationParams()

            val tiltakstyper = tiltakstypeService.getAll(filter, pagination)

            call.respond(tiltakstyper)
        }
        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            call.respond(tiltakstype)
        }

        get("{id}/faneinnhold") {
            val id = call.parameters.getOrFail<UUID>("id")
            val tiltakstype = tiltakstypeService.getById(id) ?: return@get call.respondText(
                "Det finnes ikke noe tiltakstype med id $id",
                status = HttpStatusCode.NotFound,
            )

            val veilederflateTiltakstype: VeilederflateTiltakstype = veilederflateService.hentTiltakstyper()
                .find { UUID.fromString(it.sanityId) == tiltakstype.sanityId }
                ?: return@get call.respondText(
                    "Det finnes ikke noe faneinnhold for tiltakstype med id $id",
                    status = HttpStatusCode.NotFound,
                )

            call.respond(veilederflateTiltakstype)
        }

        get("migrerte") {
            call.respond(migrerteTiltak.map { toArenaKode(it) })
        }
    }
}

data class TiltakstypeFilter(
    val sortering: String? = null,
)

fun <T : Any> PipelineContext<T, ApplicationCall>.getTiltakstypeFilter(): TiltakstypeFilter {
    val sortering = call.request.queryParameters["sort"]
    return TiltakstypeFilter(
        sortering = sortering,
    )
}
