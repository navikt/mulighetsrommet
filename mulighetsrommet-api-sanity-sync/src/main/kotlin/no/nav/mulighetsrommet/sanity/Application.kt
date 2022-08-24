package no.nav.mulighetsrommet.sanity

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.DatabaseAdapter
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.domain.models.Innsatsgruppe
import no.nav.mulighetsrommet.hoplite.loadConfiguration

fun main() {
    val config = loadConfiguration<Config>()

    val db = DatabaseAdapter(config.db)

    val result = queryOf("select id, navn from innsatsgruppe")
        .map { it.toInnsatsgruppe() }
        .asList
        .let { db.run(it) }

    println(result)
}

fun Row.toInnsatsgruppe(): Innsatsgruppe = Innsatsgruppe(
    id = int("id"),
    navn = string("navn"),
)

data class Config(
    val db: DatabaseConfig
)
