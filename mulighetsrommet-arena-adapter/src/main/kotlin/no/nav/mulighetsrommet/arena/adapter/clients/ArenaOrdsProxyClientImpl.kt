package no.nav.mulighetsrommet.arena.adapter.clients

import arrow.core.Either
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class ArenaOrdsProxyClientImpl(
    engine: HttpClientEngine = CIO.create(),
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) : ArenaOrdsProxyClient {

    private val log = LoggerFactory.getLogger(javaClass)

    val client = httpJsonClient(engine).config {
        install(HttpCache)
    }

    override suspend fun getArbeidsgiver(arbeidsgiverId: Int): Either<ResponseException, ArenaOrdsArrangor?> {
        val response = client.get("$baseUrl/ords/arbeidsgiver") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
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

    override suspend fun getFnr(personId: Int): Either<ResponseException, ArenaOrdsFnr?> {
        val response = client.get("$baseUrl/ords/fnr") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
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
