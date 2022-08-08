package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.Masked
import no.nav.mulighetsrommet.ktor.ServerConfig

data class Config(
    val server: ServerConfig,
    val app: AppConfig
)

data class AppConfig(
    val ords: ArenaOrdsConfig
)

data class ArenaOrdsConfig(
    val url: String,
    val clientId: String,
    val clientSecret: Masked
)
