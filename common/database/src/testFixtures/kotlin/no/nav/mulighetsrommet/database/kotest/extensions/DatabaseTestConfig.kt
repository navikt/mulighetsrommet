package no.nav.mulighetsrommet.database.kotest.extensions

import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.Password
import org.postgresql.util.PSQLException
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

fun createDatabaseIfNotExists(config: DatabaseConfig) {
    try {
        Database(createDatabaseConfig()).use {
            it.run(queryOf("create database \"${config.name}\"").asExecute)
        }
    } catch (e: PSQLException) {
        // This error code is expected (https://www.postgresql.org/docs/8.2/errcodes-appendix.html)
        val errorCodeDuplucateDatabase = "42P04"
        if (e.sqlState != errorCodeDuplucateDatabase) {
            // Otherwise rethrow
            throw e
        }
    }
}

fun dropDatabaseIfExists(config: DatabaseConfig) {
    Database(createDatabaseConfig()).use {
        it.run(queryOf("drop database if exists \"${config.name}\"").asExecute)
    }
}
