package no.nav.mulighetsrommet.api.persistence

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig)
