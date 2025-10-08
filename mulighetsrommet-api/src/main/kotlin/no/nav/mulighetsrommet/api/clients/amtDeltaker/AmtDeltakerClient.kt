package no.nav.mulighetsrommet.api.clients.amtDeltaker

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.pdl.PdlGradering
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.securelog.SecureLog
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import java.time.LocalDate
import java.util.*

class AmtDeltakerClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = SecureLog.logger
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
                log.error("Feil ved henting av deltakelser for bruker. Response=$bodyAsText")
                AmtDeltakerError.Error.left()
            }
        }
    }

    suspend fun hentPersonalia(
        deltakerIds: List<UUID>,
    ): Either<AmtDeltakerError, List<DeltakerPersonalia>> {
        val response = client.post("$baseUrl/external/deltakere/personalia") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(deltakerIds)
            setBody(Json.encodeToJsonElement(ListSerializer(UUIDSerializer), deltakerIds))
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body<List<DeltakerPersonaliaResponse>>()
                .map {
                    val fornavnOgMellomnavn = listOfNotNull(it.fornavn, it.mellomnavn).joinToString(" ")
                    val navn = listOf(it.etternavn, fornavnOgMellomnavn).joinToString(", ")

                    DeltakerPersonalia(
                        deltakerId = it.deltakerId,
                        norskIdent = NorskIdent(it.personident),
                        navn = navn,
                        oppfolgingEnhet = it.navEnhetsnummer?.let { NavEnhetNummer(it) },
                        erSkjermet = it.erSkjermet,
                        adressebeskyttelse = it.adressebeskyttelse ?: PdlGradering.UGRADERT,
                    )
                }
                .right()
            HttpStatusCode.NotFound -> AmtDeltakerError.Error.left()
            HttpStatusCode.BadRequest -> AmtDeltakerError.BadRequest.left()
            else -> {
                val bodyAsText = response.bodyAsText()
                log.error("Feil ved henting av personalia for deltakelser. Response=$bodyAsText")
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
        val tiltakskode: GruppeTiltakstype,
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
enum class GruppeTiltakstype {
    INDOPPFAG,
    ARBFORB,
    AVKLARAG,
    VASV,
    ARBRRHDAG,
    DIGIOPPARB,
    JOBBK,
    GRUPPEAMO,
    GRUFAGYRKE,
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
