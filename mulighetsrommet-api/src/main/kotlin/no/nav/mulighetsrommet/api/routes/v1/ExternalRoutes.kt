package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.toUUID
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import org.koin.ktor.ext.inject

fun Route.externalRoutes() {
    val tiltaksgjennomforinger: TiltaksgjennomforingRepository by inject()

    route("/api/v1") {
        get("tiltaksgjennomforinger/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: return@get call.respondText(
                "Mangler eller ugyldig id",
                status = HttpStatusCode.BadRequest
            )
            val tiltaksgjennomforing =
                tiltaksgjennomforinger.get(id) ?: return@get call.respondText(
                    "Det finnes ikke noe tiltaksgjennomføring med id $id",
                    status = HttpStatusCode.NotFound
                )
            call.respond(TiltaksgjennomforingDto.from(tiltaksgjennomforing))
        }
    }
}
