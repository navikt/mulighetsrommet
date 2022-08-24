package no.nav.mulighetsrommet.sanity

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.hoplite.loadConfiguration

fun main() {
    val (db) = loadConfiguration<Config>()

    println(db)
}

data class Config(
    val db: DatabaseConfig
)
