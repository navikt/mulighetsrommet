package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.mulighetsrommet.api.database.DatabaseFactory

// TODO: Fix this for the better. Might be better options to control the migration outside of the application scope per environment.
fun Route.devRoutes() {
    get("internal/dev/db/clean-migrate") {
        val numOfCleaned = DatabaseFactory.cleanDatabase()
        val numfOfMigrations = DatabaseFactory.migrateDatabase()
        call.respondText(
            "Antall vaskede tabeller og migreringer: $numOfCleaned - $numfOfMigrations",
            status = HttpStatusCode.NoContent
        )
    }
    get("internal/dev/db/clean") {
        val numOfCleaned = DatabaseFactory.cleanDatabase()
        call.respondText("Antall vaskede tabeller: $numOfCleaned", status = HttpStatusCode.NoContent)
    }
    get("internal/dev/db/migrate") {
        val numOfMigrations = DatabaseFactory.migrateDatabase()
        call.respondText("Antall migreringer fullført: $numOfMigrations", status = HttpStatusCode.NoContent)
    }
    get("internal/dev/db/add-test-data") {
        call.respondText("Not implemented", status = HttpStatusCode.NotImplemented)
    }
}
