package no.nav.mulighetsrommet.ktor.clients

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.metrics.Metrikker
import org.slf4j.LoggerFactory
import org.slf4j.MDC

val ClientResponseMetricPlugin = createClientPlugin("ClientResponseMetricPlugin") {
    onResponse { response ->
        Metrikker.clientResponseMetrics(response.call.request.url.host, response.status.value).increment()
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
            },
        )
    }

    install(ClientResponseMetricPlugin)

    defaultRequest {
        contentType(ContentType.Application.Json)

        System.getenv("NAIS_APP_NAME")?.let {
            header("Nav-Consumer-Id", it)
        }

        MDC.get("correlationId")?.let {
            header("Nav-Call-Id", it)
        }
    }
}
