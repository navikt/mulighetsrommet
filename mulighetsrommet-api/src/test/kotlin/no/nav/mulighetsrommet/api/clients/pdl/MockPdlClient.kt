package no.nav.mulighetsrommet.api.clients.pdl

import io.ktor.client.engine.mock.*

fun mockPdlClient(clientEngine: MockEngine) = PdlClient(
    config = PdlClient.Config(baseUrl = "https://pdl.no"),
    tokenProvider = { "token" },
    clientEngine = clientEngine,
)
