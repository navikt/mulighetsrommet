package no.nav.mulighetsrommet.tiltakshistorikk

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig)
