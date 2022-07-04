package no.nav.mulighetsrommet.api.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

class BrukerService {
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        // expectSuccess = true
        defaultRequest {
            url("https://veilarbvedtaksstotte.dev-fss-pub.nais.io/veilarbvedtaksstotte/api/") // TODO Må være basert på miljø (hente fra property-fil?)
        }
    }

    suspend fun hentBrukerdata(fnr: String): Brukerdata {
        val response = client.get("siste-14a-vedtak?fnr=$fnr")
        response.let {
            return it.body()
        }
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe,
)

enum class Innsatsgruppe {
    STANDARD_INNSATS, SITUASJONSBESTEMT_INNSATS, SPESIELT_TILPASSET_INNSATS, GRADERT_VARIG_TILPASSET_INNSATS, VARIG_TILPASSET_INNSATS
}
