package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.domain.dbo.Utkasttype
import no.nav.mulighetsrommet.api.domain.dto.UtkastDto
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.UtkastService
import no.nav.mulighetsrommet.api.utils.UtkastFilter
import org.koin.ktor.ext.inject
import java.util.*

fun Route.utkastRoutes() {
    val utkastService: UtkastService by inject()

    route("/api/v1/internal/utkast") {
        get {
            val type = Utkasttype.valueOf(call.request.queryParameters.getOrFail("utkasttype"))
            val opprettetAv = call.request.queryParameters["opprettetAv"]
            val avtaleId = call.request.queryParameters.getOrFail<UUID>("avtaleId")

            call.respondWithStatusResponse(utkastService.getAll(filter = UtkastFilter(type = type, opprettetAv = opprettetAv, avtaleId = avtaleId)))
        }

        get("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(utkastService.get(id))
        }

        put {
            val utkastData = call.receive<UtkastDto>()
            call.respondWithStatusResponse(utkastService.upsert(utkastData.toDbo()))
        }

        delete("{id}") {
            val id = call.parameters.getOrFail<UUID>("id")
            call.respondWithStatusResponse(utkastService.deleteUtkast(id))
        }
    }
}
