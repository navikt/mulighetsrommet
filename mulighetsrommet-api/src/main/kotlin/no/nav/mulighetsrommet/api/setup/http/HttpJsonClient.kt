package no.nav.mulighetsrommet.api.setup.http

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Creates a [HttpClient] preconfigured with support for serialization and deserialization of
 * JSON request- and response-payloads.
 *
 * The client does not throw exceptions on unsuccessful HTTP responses by default, but the default
 * behavior can be overridden by implementing a custom setup [block].
 */
fun createHttpJsonClient(
    engine: HttpClientEngine = CIO.create(),
    block: HttpClientConfig<*>.() -> Unit = {},
): HttpClient {
    return HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }

        block()
    }
}
