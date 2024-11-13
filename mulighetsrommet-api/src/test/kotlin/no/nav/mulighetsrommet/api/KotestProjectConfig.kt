package no.nav.mulighetsrommet.api

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig)
