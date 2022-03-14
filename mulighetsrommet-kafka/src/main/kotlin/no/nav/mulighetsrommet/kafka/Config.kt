package no.nav.mulighetsrommet.kafka

import com.sksamuel.hoplite.Masked

data class AppConfig(
    val env: String,
    val server: ServerConfig,
    val database: DatabaseConfig
)

data class ServerConfig(
    val host: String,
    val port: Int
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: Masked
)
