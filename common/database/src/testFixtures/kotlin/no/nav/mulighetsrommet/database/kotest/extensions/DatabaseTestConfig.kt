package no.nav.mulighetsrommet.database.kotest.extensions

import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun createDatabaseConfig(
    name: String = "valp",
    port: Int = 5442,
    host: String = "localhost",
    user: String = "valp",
    password: Password = Password("valp"),
): DatabaseConfig {
    return DatabaseConfig(
        jdbcUrl = "jdbc:postgresql://$host:$port/$name?user=$user&password=${password.value}",
        schema = null,
        maximumPoolSize = 2,
    )
}

fun createRandomDatabaseConfig(
    prefix: String = "valp",
    port: Int = 5442,
    host: String = "localhost",
    user: String = "valp",
    password: Password = Password("valp"),
): DatabaseConfig {
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    val randomId = (1..8).map { ('a'..'z').random() }.joinToString("")
    val randomDatabaseName = "$prefix-$currentTime-$randomId"
    return createDatabaseConfig(randomDatabaseName, port, host, user, password)
}
