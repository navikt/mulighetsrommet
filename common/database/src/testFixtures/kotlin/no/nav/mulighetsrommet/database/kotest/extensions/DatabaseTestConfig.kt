package no.nav.mulighetsrommet.database.kotest.extensions

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import java.util.*

fun createDatabaseTestSchema(
    name: String,
    port: Int,
    host: String = "localhost",
    user: String = "valp",
    password: Password = Password("valp")
): DatabaseConfig {
    val schema = "$port--${UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password, 1)
}
