package no.nav.mulighetsrommet.utdanning

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.utdanning.db.UtdanningQueries
import org.koin.ktor.ext.inject

fun Route.utdanningRoutes() {
    val db: Database by inject()

    route("utdanninger") {
        get {
            val utdanninger = db.useSession {
                UtdanningQueries.getUtdanningsprogrammer(it)
            }
            call.respond(utdanninger)
        }
    }
}
