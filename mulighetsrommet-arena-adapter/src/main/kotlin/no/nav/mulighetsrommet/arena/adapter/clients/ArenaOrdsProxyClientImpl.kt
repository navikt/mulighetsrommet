package no.nav.mulighetsrommet.arena.adapter.clients

import arrow.core.Either
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.models.dto.Arrangor
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class ArenaOrdsProxyClientImpl(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: () -> String,
) : ArenaOrdsProxyClient {

    private val log = LoggerFactory.getLogger(javaClass)

    val client = HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        defaultRequest {
            header("Nav-Consumer-Id", "mulighetsrommet-arena-adapter")
            MDC.get("call-id")?.let { header(HttpHeaders.XRequestId, it) }
        }

        install(HttpCache)
    }

    override suspend fun getArbeidsgiver(arbeidsgiverId: Int): Either<ResponseException, Arrangor?> {
        val response = client.get("$baseUrl/ords/arbeidsgiver") {
            bearerAuth(tokenProvider.invoke())
            parameter("arbeidsgiverId", arbeidsgiverId)
        }

        return when (response.status) {
            HttpStatusCode.OK -> Either.Right(response.body())

            HttpStatusCode.NotFound -> {
                log.warn("Fant ikke arrangørinfo for arrangør id: $arbeidsgiverId")
                Either.Right(null)
            }

            else -> Either.Left(ResponseException(response, "Unexpected response from arena-ords-proxy"))
        }
    }

    override suspend fun getFnr(personId: Int): Either<ResponseException, String?> {
        val response = client.get("$baseUrl/ords/fnr") {
            bearerAuth(tokenProvider.invoke())
            parameter("personId", personId)
        }

        return when (response.status) {
            HttpStatusCode.OK -> Either.Right(response.body())

            HttpStatusCode.NotFound -> {
                log.warn("Fant ikke fødselsnummer for Arena personId: $personId")
                Either.Right(null)
            }

            else -> Either.Left(ResponseException(response, "Unexpected response from arena-ords-proxy"))
        }
    }
}
