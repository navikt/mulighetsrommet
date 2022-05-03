package no.nav.mulighetsrommet.api

import com.sksamuel.hoplite.Masked

data class Config(
    val server: ServerConfig,
    val app: AppConfig,
)

data class ServerConfig(
    val host: String,
    val port: Int
)

data class AppConfig(
    val database: DatabaseConfig,
    val auth: Map<String, AuthProvider>
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val schema: String?,
    val user: String,
    val password: Masked
)

data class AuthProvider(
    val issuer: String,
    val jwksUri: String,
    val audience: String
)
