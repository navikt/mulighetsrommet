package no.nav.tiltak.okonomi.api

import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.maam.Maam

fun Route.maamRoutes(kafka: KafkaConsumerOrchestrator, db: Database) = authenticate {
    route("/maam") {
        install(Maam) {
            database = { db }
            kafka(orchestrator = { kafka }) {
                consumers()
                failedRecords()
            }
        }
    }
}
