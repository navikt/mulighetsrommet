package no.nav.mulighetsrommet.utils

import io.ktor.client.engine.mock.MockEngine
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient

fun mockPdlClient(clientEngine: MockEngine) = PdlClient(
    config = PdlClient.Config(baseUrl = "https://pdl.no"),
    tokenProvider = { "token" },
    clientEngine = clientEngine,
)
