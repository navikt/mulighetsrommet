package no.nav.mulighetsrommet.api.clients.dokark

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory

class DokarkClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun opprettJournalpost(journalpost: Journalpost, accessType: AccessType): DokarkResult {
        val response = client.post("$baseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true") {
            bearerAuth(tokenProvider.exchange(accessType))
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(journalpost))
        }

        if (!response.status.isSuccess()) {
            log.warn("Feilet å opprette journalpost: {}", response.bodyAsText())
            return DokarkResult.Error("Feilet å laste opp til joark")
        }

        val dokarkresponse = response.body<DokarkResponse>()
        if (!dokarkresponse.journalpostferdigstilt) {
            log.warn("Opprettet journalpost, men den kunne ikke bli ferdigstilt automatisk")
        }
        return DokarkResult.Success(journalpostId = dokarkresponse.journalpostId)
    }
}

@Serializable
data class DokarkResponse(
    val journalpostId: String,
    val journalstatus: String,
    val melding: String?,
    val journalpostferdigstilt: Boolean,
    val dokumenter: List<DokarkResponseDokument>,
)

@Serializable
data class DokarkResponseDokument(
    val dokumentInfoId: String,
)

sealed interface DokarkResult {
    data class Success(val journalpostId: String) : DokarkResult
    data class Error(val message: String) : DokarkResult
}

@Serializable
data class Journalpost(
    val tittel: String,
    val avsenderMottaker: AvsenderMottaker,
    val bruker: Bruker?,
    val tema: String,
    val behandlingstema: String?,
    val datoMottatt: String,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: String,
    val journalfoerendeEnhet: String,
    val journalposttype: String,
    val sak: Sak?,
) {
    @Serializable
    data class AvsenderMottaker(
        val id: String,
        val idType: String,
        val navn: String?,
    )

    @Serializable
    data class Bruker(
        val id: String,
        val idType: String,
    )

    @Serializable
    data class Sak(
        val fagsakId: String,
        val fagsaksystem: String,
        val sakstype: String,
    )

    @Serializable
    data class Dokument(
        val dokumentvarianter: List<Dokumentvariant>,
        val tittel: String,
    ) {
        @Serializable
        data class Dokumentvariant(
            val filtype: String,
            val fysiskDokument: ByteArray,
            val variantformat: String,
        )
    }
}
