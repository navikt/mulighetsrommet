package no.nav.tiltak.historikk

import arrow.core.Either
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import java.util.UUID

class TiltakshistorikkClient(
    clientEngine: HttpClientEngine,
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    object OkResponse

    suspend fun getHistorikk(
        identer: List<NorskIdent>,
        years: Int? = null,
    ): Either<ResponseException, TiltakshistorikkV1Response> {
        val response = client.post("$baseUrl/api/v1/historikk") {
            setBody(TiltakshistorikkV1Request(identer, maxAgeYears = years))
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }
        return onSuccess(response) { response.body() }
    }

    suspend fun upsertArenaGjennomforing(dbo: TiltakshistorikkArenaGjennomforing): Either<ResponseException, OkResponse> {
        val response = client.put("$baseUrl/api/v1/intern/arena/gjennomforing") {
            setBody(dbo)
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }
        return onSuccess(response) { OkResponse }
    }

    suspend fun deleteArenaGjennomforing(id: UUID): Either<ResponseException, OkResponse> {
        val response = client.delete("$baseUrl/api/v1/intern/arena/gjennomforing/$id") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }
        return onSuccess(response) { OkResponse }
    }

    suspend fun upsertArenaDeltaker(dbo: TiltakshistorikkArenaDeltaker): Either<ResponseException, OkResponse> {
        val response = client.put("$baseUrl/api/v1/intern/arena/deltaker") {
            setBody(dbo)
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }
        return onSuccess(response) { OkResponse }
    }

    suspend fun deleteArenaDeltaker(id: UUID): Either<ResponseException, OkResponse> {
        val response = client.delete("$baseUrl/api/v1/intern/arena/deltaker/$id") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }
        return onSuccess(response) { OkResponse }
    }

    private suspend fun <T> onSuccess(
        response: HttpResponse,
        handleSuccess: suspend () -> T,
    ): Either<ResponseException, T> {
        return if (response.status.isSuccess()) {
            try {
                Either.Right(handleSuccess())
            } catch (e: Throwable) {
                Either.Left(ResponseException(response, e.message ?: "Ukjent feil"))
            }
        } else {
            Either.Left(ResponseException(response, response.bodyAsText()))
        }
    }
}
