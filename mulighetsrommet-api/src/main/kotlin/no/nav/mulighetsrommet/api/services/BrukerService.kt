package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import org.slf4j.LoggerFactory

class BrukerService {
    private val log = LoggerFactory.getLogger(this.javaClass)
    val tokenClient = AzureAdTokenClientBuilder.builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient()

    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            url("https://veilarbvedtaksstotte.dev-fss-pub.nais.io/veilarbvedtaksstotte/api/") // TODO Må være basert på miljø (hente fra property-fil?)
        }
    }

    private val oppfolgingClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            url("https://veilarboppfolging.dev-fss-pub.nais.io/veilarboppfolging/api/") // TODO Må være basert på miljø (hente fra property-fil?)
        }
    }

    suspend fun hentBrukerdata(fnr: String): Brukerdata {
        val innsatsgruppe = hentSiste14aVedtak(fnr)
        val oppfolgingsenhet = hentOppfolgingsenhet(fnr)

        return Brukerdata(
            fnr = fnr,
            oppfolgingsenhet = oppfolgingsenhet,
            innsatsgruppe = innsatsgruppe
        )
    }

    private suspend fun hentSiste14aVedtak(fnr: String): Innsatsgruppe {
        val response = client.get("person/$fnr/oppfolgingsstatus")
        if (response.status == HttpStatusCode.OK) {
            response.let {
                val data = it.body<Innsatsgruppe>()
                log.info("Hentet oppfølgingsstatus: {}", data)
                return data
            }
        }
        log.info("Fant ikke oppfølgingsstatus for bruker med fnr: $fnr - Response status: {}", response.status)
        throw NotFoundException()
    }

    private suspend fun hentOppfolgingsenhet(fnr: String): Oppfolgingsenhet {
        val response = oppfolgingClient.get("siste-14a-vedtak?fnr=$fnr")
        if (response.status == HttpStatusCode.OK) {
            response.let {
                val data = it.body<Oppfolgingsenhet>()
                log.info("Hentet siste 14a-vedtak: {}", data)
                return data
            }
        }
        log.info("Fant ikke siste 14a-vedtak for bruker med fnr: $fnr - Response status: {}", response.status)
        throw NotFoundException(message = "Fant ikke siste 14a-vedtak for bruker med fnr: $fnr")
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe,
    val oppfolgingsenhet: Oppfolgingsenhet
)

enum class Innsatsgruppe {
    STANDARD_INNSATS, SITUASJONSBESTEMT_INNSATS, SPESIELT_TILPASSET_INNSATS, GRADERT_VARIG_TILPASSET_INNSATS, VARIG_TILPASSET_INNSATS
}

@Serializable
data class Oppfolgingsenhet(
    val enhetId: String,
    val navn: String
)
