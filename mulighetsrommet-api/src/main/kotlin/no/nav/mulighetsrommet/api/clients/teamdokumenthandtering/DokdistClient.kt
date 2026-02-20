package no.nav.mulighetsrommet.api.clients.teamdokumenthandtering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

/**
 * Se https://confluence.adeo.no/spaces/BOA/pages/320039012/POST+rest+v1+distribuerjournalpost
 */
class DokdistClient(
    clientEngine: HttpClientEngine,
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun distribuerJournalpost(
        journalpostId: String,
        accessType: AccessType,
        distribusjonstype: DokdistRequest.DistribusjonsType,
        adresse: DokdistRequest.Adresse?,
        batchId: String? = null,
    ): Either<DokdistError, DokdistResponse> {
        val request = DokdistRequest(
            journalpostId = journalpostId,
            batchId = batchId,
            adresse = adresse,
            distribusjonstype = distribusjonstype,
            distribusjonstidspunkt = if (accessType == AccessType.M2M) {
                DokdistRequest.Distribusjonstidspunkt.KJERNETID
            } else {
                DokdistRequest.Distribusjonstidspunkt.UMIDDELBART
            },
        )
        val response = client.post("$baseUrl/rest/v1/distribuerjournalpost") {
            bearerAuth(tokenProvider.exchange(accessType))
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        if (response.status.isSuccess()) {
            return response.body<DokdistResponse>().right()
        }

        if (response.status == HttpStatusCode.Conflict) {
            log.warn("Journalposten er allerede under distribusjon: $journalpostId")
            return response.body<DokdistResponse>().right()
        }

        return DokdistError.from(response).left()
    }
}

@Serializable
data class DokdistRequest(
    val journalpostId: String,
    val batchId: String?,
    val bestillendeFagsystem: String = "TILTAKSADMINISTRASJON",
    val adresse: Adresse?,
    val dokumentProdApp: String = "TILTAKSADMINISTRASJON",
    val distribusjonstype: DistribusjonsType,
    val distribusjonstidspunkt: Distribusjonstidspunkt,
) {
    /**
     * Struktur for å beskrive postadresse. Inneholder enten norsk postadresse eller utenlandsk postadresse.
     * Påkrevd hvis mottaker er samhandler, ellers skal dokdistsentralprint hente postadresse fra fellesregistre hvis ikke satt.
     * For utenlandsk postadresse skal postnummer og poststed feltene være tomme. Eventuelle tilsvarende postnummer og sted plasseres i adresselinjene.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonClassDiscriminator("adressetype")
    sealed class Adresse {
        abstract val postnummer: String?
        abstract val poststed: String?
        abstract val adresselinje1: String?
        abstract val adresselinje2: String?
        abstract val adresselinje3: String?

        /**
         * iso3166-1 alfa-2
         *
         * Ex: "NO", "SE", etc.
         */
        abstract val land: String?

        @Serializable
        @SerialName("norskPostadresse")
        data class NorskPostAdresse(
            override val postnummer: String,
            override val poststed: String,
            override val land: String = "NO",
            override val adresselinje1: String?,
            override val adresselinje2: String?,
            override val adresselinje3: String?,
        ) : Adresse()

        @Serializable
        @SerialName("utenlandskPostadresse")
        data class UtenlandskPostadresse(
            override val land: String,
            override val adresselinje1: String,
            override val adresselinje2: String?,
            override val adresselinje3: String?,
        ) : Adresse() {
            override val postnummer: String? = null
            override val poststed: String? = null
        }
    }

    @Serializable
    enum class DistribusjonsType {
        VEDTAK,
        VIKTIG,
        ANNET,
    }

    @Serializable
    enum class Distribusjonstidspunkt {
        UMIDDELBART,
        KJERNETID,
    }
}

data class DokdistResponse(val bestillingsId: String)

sealed class DokdistError(open val message: String) {

    /**
     * 400 Bad Request
     *
     * Validering av request body, eller validering av journalposten som journalpostId refererer til feilet.
     */
    data class BadRequestError(override val message: String) : DokdistError(message)

    /**
     * 401 Unauthorized
     *
     *  - Bruker mangler tilgang for å vise journalposten.
     *  - Ugyldig OIDC token.
     */
    data class UnauthorizedError(override val message: String) : DokdistError(message)

    /**
     * 404 Not Found
     *
     * Journalposten ble ikke funnet.
     */
    data class NotFoundError(override val message: String) : DokdistError(message)

    /**
     * 410 Gone
     *
     * Journalpost kan ikke distribueres. Bruker er død og har ukjent postadresse.
     */
    data class GoneError(override val message: String) : DokdistError(message)

    /**
     * 500 Internal Server Error
     *
     * Teknisk feil under prosessering av forsendelse.
     */
    data class InternalServerError(override val message: String) : DokdistError(message)

    /**
     * 503 Bad Gateway
     *
     * Teknisk feil ved kall mot ekstern tjeneste
     */
    data class BadGatewayError(override val message: String) :
        DokdistError(message)

    data class UnknownError(override val message: String) : DokdistError(message)

    companion object {
        fun from(httpResponse: HttpResponse): DokdistError = when (httpResponse.status) {
            HttpStatusCode.BadRequest -> BadRequestError("Feil i request body eller journalposten som journalpostId refererer til")
            HttpStatusCode.Unauthorized -> UnauthorizedError("Ugyldig OIDC token eller manglende tilgang for å vise journalposten")
            HttpStatusCode.NotFound -> NotFoundError("Journalposten ble ikke funnet")
            HttpStatusCode.Gone -> GoneError("Journalpost kan ikke distribueres. Bruker er død og har ukjent postadresse")
            HttpStatusCode.InternalServerError -> InternalServerError("Teknisk feil under prosessering av forsendelse")
            HttpStatusCode.BadGateway -> BadGatewayError("Teknisk feil ved kall mot ekstern tjeneste")
            else -> UnknownError("Ukjent feil ved distribusjon av journalpost: ${httpResponse.status.value} ${httpResponse.status.description}")
        }
    }
}
