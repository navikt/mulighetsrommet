package no.nav.mulighetsrommet.database.kotest.extensions

import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.database.Password
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun createDatabaseTestSchema(
    name: String,
    port: Int,
    host: String = "localhost",
    user: String = "valp",
    password: Password = Password("valp"),
): FlywayDatabaseAdapter.Config {
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss"))
    val schema = "$currentTime:${UUID.randomUUID()}"
    return FlywayDatabaseAdapter.Config(
        host,
        port,
        name,
        schema,
        user,
        password,
        2,
        migrationConfig = FlywayDatabaseAdapter.MigrationConfig(cleanDisabled = false),
    )
}
