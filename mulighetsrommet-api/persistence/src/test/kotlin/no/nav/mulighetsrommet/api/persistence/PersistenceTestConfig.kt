package no.nav.mulighetsrommet.api.persistence

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig

val databaseConfig: DatabaseConfig = createRandomDatabaseConfig("mr-api")

val outboxConfig = OutboxTopics(
    sisteTiltakstyperV3 = "siste-tiltakstyper-v3",
    totrinnskontrollHendelseV1 = "totrinnskontroll-hendelse-v1",
)
