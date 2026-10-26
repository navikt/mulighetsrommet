package no.nav.mulighetsrommet.admin

import no.nav.mulighetsrommet.api.persistence.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig)
