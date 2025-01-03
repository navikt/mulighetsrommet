package no.nav.mulighetsrommet.utdanning

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.database.Database
import org.koin.ktor.ext.inject

fun Route.utdanningRoutes() {
    val db: Database by inject()

    route("utdanninger") {
        get {
            val utdanninger = db.session {
                Queries.utdanning.getUtdanningsprogrammer()
            }
            call.respond(utdanninger)
        }
    }
}
