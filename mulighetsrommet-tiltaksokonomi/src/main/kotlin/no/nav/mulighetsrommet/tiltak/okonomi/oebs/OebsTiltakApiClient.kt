package no.nav.mulighetsrommet.tiltak.okonomi.oebs

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.TokenProvider

class OebsTiltakApiClient(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    private val client = httpJsonClient(engine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 0, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    fun getBestillingStatus(bestillingId: String) = OebsBestilling(
        id = bestillingId,
        status = OebsBestilling.Status.BEHANDLET,
    )
}
