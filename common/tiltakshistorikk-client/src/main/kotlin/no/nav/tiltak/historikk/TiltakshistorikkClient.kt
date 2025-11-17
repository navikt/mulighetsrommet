package no.nav.tiltak.historikk

import arrow.core.Either
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import java.util.*

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
            Either.Right(handleSuccess())
        } else {
            Either.Left(ResponseException(response, response.bodyAsText()))
        }
    }
}
