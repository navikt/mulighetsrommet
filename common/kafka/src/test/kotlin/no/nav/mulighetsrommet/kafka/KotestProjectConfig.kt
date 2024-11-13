package no.nav.mulighetsrommet.kafka

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig

val testDatabaseConfig = createRandomDatabaseConfig()

class KotestProjectConfig : CreateDatabaseTestListener(testDatabaseConfig)
