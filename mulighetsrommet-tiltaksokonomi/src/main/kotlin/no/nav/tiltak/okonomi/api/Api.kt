package no.nav.tiltak.okonomi.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.tiltak.okonomi.service.OkonomiService

const val API_BASE_PATH = "/api/v1/okonomi"

fun Application.configureApi(
    kafka: KafkaConsumerOrchestrator,
    okonomi: OkonomiService,
    db: Database,
) = routing {
    install(Resources)

    maamRoutes(kafka, db)

    oebsRoutes(okonomi)
}
