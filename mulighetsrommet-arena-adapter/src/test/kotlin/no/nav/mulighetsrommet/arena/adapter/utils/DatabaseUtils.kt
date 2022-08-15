package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.utils

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import java.util.*

fun createDatabaseConfigWithRandomSchema(
    host: String = "localhost",
    port: Int = 5443,
    name: String = "mulighetsrommet-arena-adapter-db",
    user: String = "valp",
    password: Password = Password("valp")
): DatabaseConfig {
    val schema = "${UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password, 1)
}
