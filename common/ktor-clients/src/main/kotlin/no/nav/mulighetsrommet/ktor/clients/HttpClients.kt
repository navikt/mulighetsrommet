package no.nav.mulighetsrommet.ktor.clients

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.metrics.Metrics
import org.slf4j.LoggerFactory

val ClientResponseMetricPlugin = createClientPlugin("ClientResponseMetricPlugin") {
    onResponse { response ->
        Metrics.clientResponseMetrics(response.call.request.url.host, response.status.value).increment()
    }
}

fun httpJsonClient(engine: HttpClientEngine = CIO.create()) = HttpClient(engine) {
    expectSuccess = false

    install(Logging) {
        level = LogLevel.INFO

        logger = object : Logger {
            private val logger = LoggerFactory.getLogger(HttpClient::class.java)

            private val fnrRegex = "\\d{11}".toRegex()

            override fun log(message: String) {
                val maskedMessage = message.replace(fnrRegex, replacement = "***********")
                logger.info(maskedMessage)
            }
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            },
        )
    }

    install(ClientResponseMetricPlugin)

    defaultRequest {
        contentType(ContentType.Application.Json)

        System.getenv("NAIS_APP_NAME")?.let {
            header("Nav-Consumer-Id", it)
        }
    }
}
