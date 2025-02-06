package no.nav.mulighetsrommet.ktor

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
)
