package no.nav.mulighetsrommet.database.kotest.extensions

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun createDatabaseTestSchema(
    name: String = "valp",
    port: Int = 5442,
    host: String = "localhost",
    user: String = "valp",
    password: Password = Password("valp"),
): DatabaseConfig {
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    val schema = "$currentTime-${UUID.randomUUID()}"
    return DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://$host:$port/$name?user=$user&password=${password.value}",
        schema = schema,
        maximumPoolSize = 2,
    )
}
