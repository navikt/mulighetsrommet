package no.nav.mulighetsrommet.maam

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig

val testDatabaseConfig = createRandomDatabaseConfig("maam")

class KotestProjectConfig : CreateDatabaseTestListener(testDatabaseConfig)
