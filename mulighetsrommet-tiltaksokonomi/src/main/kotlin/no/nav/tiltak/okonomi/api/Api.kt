package no.nav.tiltak.okonomi.api

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.oebs.OebsService

const val API_BASE_PATH = "/api/v1/okonomi"

fun Application.configureApi(
    kafka: KafkaConsumerOrchestrator,
    db: OkonomiDatabase,
    oebs: OebsService,
) = routing {
    install(Resources)

    maamRoutes(kafka)

    bestillingRoutes(db, oebs)
    fakturaRoutes(db, oebs)
}
