package no.nav.mulighetsrommet.utdanning

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.ApiDatabase
import org.koin.ktor.ext.inject

fun Route.utdanningRoutes() {
    val db: ApiDatabase by inject()

    route("utdanninger") {
        get {
            val utdanninger = db.session {
                queries.utdanning.getUtdanningsprogrammer()
            }
            call.respond(utdanninger)
        }
    }
}
