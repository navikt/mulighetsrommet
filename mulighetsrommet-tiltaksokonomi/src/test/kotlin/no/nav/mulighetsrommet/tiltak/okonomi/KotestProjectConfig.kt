package no.nav.mulighetsrommet.tiltak.okonomi

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig)
