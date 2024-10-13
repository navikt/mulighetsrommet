package no.nav.mulighetsrommet.utdanning.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.utdanning.db.UtdanningRepository
import org.koin.ktor.ext.inject

fun Route.utdanningRoutes() {
    val utdanningRepository: UtdanningRepository by inject()

    route("utdanninger") {
        get {
            call.respond(utdanningRepository.getUtdanningsprogrammer())
        }
    }
}
