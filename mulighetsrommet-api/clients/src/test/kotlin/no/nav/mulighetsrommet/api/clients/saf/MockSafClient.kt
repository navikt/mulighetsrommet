package no.nav.mulighetsrommet.api.clients.saf

import io.ktor.client.engine.mock.MockEngine

fun mockSafClient(clientEngine: MockEngine) = SafClient(
    config = SafClient.Config(baseUrl = "https://saf.no"),
    tokenProvider = { "token" },
    clientEngine = clientEngine,
)
