package no.nav.mulighetsrommet.arena.adapter

import no.nav.mulighetsrommet.database.kotest.extensions.CreateDatabaseTestListener

object KotestProjectConfig : CreateDatabaseTestListener(databaseConfig)
