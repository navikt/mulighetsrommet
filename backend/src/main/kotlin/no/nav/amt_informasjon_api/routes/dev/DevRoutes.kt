package no.nav.amt_informasjon_api.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.amt_informasjon_api.database.DatabaseFactory

// Kun midlertidig i dev-miljø for å kunne styre migrering og slette DB.
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
