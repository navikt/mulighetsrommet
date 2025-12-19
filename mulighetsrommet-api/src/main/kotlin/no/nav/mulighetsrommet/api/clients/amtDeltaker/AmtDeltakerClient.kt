package no.nav.mulighetsrommet.api.clients.amtDeltaker

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.teamLogs.teamLogsError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class AmtDeltakerClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val logger = LoggerFactory.getLogger(AmtDeltakerClient::class.java)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun hentDeltakelser(
        requestBody: DeltakelserRequest,
        obo: AccessType.OBO,
    ): Either<AmtDeltakerError, DeltakelserResponse> {
        val response = client.post("$baseUrl/deltakelser") {
            bearerAuth(tokenProvider.exchange(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(requestBody)
        }

        return when (response.status) {
            HttpStatusCode.OK -> Either.Right(response.body<DeltakelserResponse>())

            HttpStatusCode.NotFound -> AmtDeltakerError.Error.left()

            HttpStatusCode.BadRequest -> AmtDeltakerError.BadRequest.left()

            else -> {
                val bodyAsText = response.bodyAsText()
                SecureLog.logger.error("Feil ved henting av deltakelser for bruker. Response=$bodyAsText")
                logger.teamLogsError("Feil ved henting av deltakelser for bruker. Response=$bodyAsText")
                AmtDeltakerError.Error.left()
            }
        }
    }

    suspend fun hentPersonalia(
        deltakerIds: Set<UUID>,
    ): Either<AmtDeltakerError, Set<DeltakerPersonalia>> {
        val response = client.post("$baseUrl/external/deltakere/personalia") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(deltakerIds)
            setBody(Json.encodeToJsonElement(ListSerializer(UUIDSerializer), deltakerIds.toList()))
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<DeltakerPersonaliaResponse>>()
                .map { personalia ->
                    val fornavnOgMellomnavn = listOfNotNull(personalia.fornavn, personalia.mellomnavn).joinToString(" ")
                    val navn = listOf(personalia.etternavn, fornavnOgMellomnavn).joinToString(", ")

                    DeltakerPersonalia(
                        deltakerId = personalia.deltakerId,
                        norskIdent = NorskIdent(personalia.personident),
                        navn = navn,
                        oppfolgingEnhet = personalia.navEnhetsnummer?.let { NavEnhetNummer(it) },
                        erSkjermet = personalia.erSkjermet,
                        adressebeskyttelse = personalia.adressebeskyttelse ?: PdlGradering.UGRADERT,
                    )
                }
                .toSet()
                .right()

            HttpStatusCode.NotFound -> AmtDeltakerError.Error.left()

            HttpStatusCode.BadRequest -> AmtDeltakerError.BadRequest.left()

            else -> {
                val bodyAsText = response.bodyAsText()
                SecureLog.logger.error("Feil ved henting av personalia for deltakelser. Response=$bodyAsText")
                logger.teamLogsError("Feil ved henting av personalia for deltakelser. Response=$bodyAsText")
                AmtDeltakerError.Error.left()
            }
        }
    }
}

enum class AmtDeltakerError {
    BadRequest,
    NotFound,
    Error,
}

@Serializable
data class DeltakelserRequest(
    val norskIdent: NorskIdent,
)

@Serializable
data class DeltakelserResponse(
    val aktive: List<DeltakelseFraKomet>,
    val historikk: List<DeltakelseFraKomet>,
) {
    @Serializable
    data class Tiltakstype(
        val navn: String,
        val tiltakskode: Tiltakskode,
    )
}

@Serializable
data class DeltakelseFraKomet(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val deltakerlisteId: UUID,
    val tittel: String,
    val tiltakstype: DeltakelserResponse.Tiltakstype,
    val status: Status,
    @Serializable(with = LocalDateSerializer::class)
    val innsoktDato: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val sistEndretDato: LocalDate? = null,
    val periode: Periode? = null,
) {

    @Serializable
    data class Status(
        val type: DeltakerStatusType,
        val visningstekst: String,
        val aarsak: String? = null,
    )

    @Serializable
    data class Periode(
        @Serializable(with = LocalDateSerializer::class)
        val startdato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val sluttdato: LocalDate?,
    )
}

@Serializable
data class DeltakerPersonaliaResponse(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val personident: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val navEnhetsnummer: String?,
    val erSkjermet: Boolean,
    val adressebeskyttelse: PdlGradering?,
)

data class DeltakerPersonalia(
    val deltakerId: UUID,
    val norskIdent: NorskIdent,
    val navn: String,
    val oppfolgingEnhet: NavEnhetNummer?,
    val erSkjermet: Boolean,
    val adressebeskyttelse: PdlGradering,
)
