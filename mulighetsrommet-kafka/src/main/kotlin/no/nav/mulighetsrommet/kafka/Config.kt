package no.nav.mulighetsrommet.kafka

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
    val port: Int
)
