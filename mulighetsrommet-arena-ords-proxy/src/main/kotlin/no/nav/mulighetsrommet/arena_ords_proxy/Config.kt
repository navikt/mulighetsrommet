package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.Masked

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class ServerConfig(
    val host: String,
    val port: Int
)

data class AppConfig(
    val ords: ArenaOrdsConfig
)

data class ArenaOrdsConfig(
    val url: String,
    val clientId: String,
    val clientSecret: Masked
)
