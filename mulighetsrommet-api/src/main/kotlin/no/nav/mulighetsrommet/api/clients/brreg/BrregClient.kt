package no.nav.mulighetsrommet.api.clients.brreg

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory

object OrgnummerUtil {
    fun erOrgnr(verdi: String): Boolean {
        val orgnrPattern = "^[0-9]{9}\$".toRegex()
        return orgnrPattern.matches(verdi)
    }
}

class BrregClient(
    private val baseUrl: String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    data class Config(
        val baseUrl: String,
    )

    suspend fun hentEnhet(orgnr: String): VirksomhetDto? {
        validerOrgnr(orgnr)
        val response = client.get("$baseUrl/enheter/$orgnr")
        val data = tolkRespons<BrregEnhet>(response, orgnr)

        if (data?.slettedato != null) {
            log.debug("Enhet med orgnr: $orgnr er slettet fra Brreg. Slettedato: ${data.slettedato}. Enhet fra Brreg: $data")
            return VirksomhetDto(
                organisasjonsnummer = data.organisasjonsnummer,
                navn = data.navn,
                overordnetEnhet = null,
                underenheter = emptyList(),
                slettedato = data.slettedato,
            )
        }

        // Vi fikk treff på hovedenhet
        if (data != null) {
            return VirksomhetDto(
                organisasjonsnummer = data.organisasjonsnummer,
                navn = data.navn,
                overordnetEnhet = null,
                underenheter = hentUnderenheterForOverordnetEnhet(orgnr),
                postnummer = data.beliggenhetsadresse?.postnummer,
                poststed = data.beliggenhetsadresse?.poststed,
            )
        }

        // Ingen treff på hovedenhet, vi sjekker underenheter også
        val underenhetResponse = client.get("$baseUrl/underenheter/$orgnr")
        val underenhetData = tolkRespons<BrregEnhet>(underenhetResponse, orgnr)

        if (underenhetData?.slettedato != null) {
            log.debug("Enhet med orgnr: $orgnr er slettet fra Brreg. Slettedato: ${underenhetData.slettedato}. Enhet fra Brreg: $data")
            return VirksomhetDto(
                organisasjonsnummer = underenhetData.organisasjonsnummer,
                navn = underenhetData.navn,
                overordnetEnhet = null,
                underenheter = null,
                slettedato = underenhetData.slettedato,
            )
        }

        return underenhetData?.let {
            VirksomhetDto(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = it.navn,
                overordnetEnhet = it.overordnetEnhet,
                underenheter = null,
                postnummer = underenhetData.beliggenhetsadresse?.postnummer,
                poststed = underenhetData.beliggenhetsadresse?.poststed,
            )
        }
    }

    suspend fun sokEtterOverordnetEnheter(orgnr: String): List<VirksomhetDto> {
        val sokEllerOppslag = when (OrgnummerUtil.erOrgnr(orgnr)) {
            true -> "organisasjonsnummer"
            false -> "navn"
        }

        val hovedenhetResponse = client.get("$baseUrl/enheter") {
            parameter("size", 20)
            parameter(sokEllerOppslag, orgnr)
        }

        val data = tolkRespons<BrregEmbeddedHovedenheter>(hovedenhetResponse, orgnr)

        return data?._embedded?.enheter?.map {
            VirksomhetDto(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = it.navn,
            )
        } ?: emptyList()
    }

    private suspend fun hentUnderenheterForOverordnetEnhet(orgnrOverordnetEnhet: String): List<VirksomhetDto> {
        validerOrgnr(orgnrOverordnetEnhet)

        val underenheterResponse = client.get("$baseUrl/underenheter") {
            parameter("size", 1000)
            parameter("overordnetEnhet", orgnrOverordnetEnhet)
        }

        val data = tolkRespons<BrregEmbeddedUnderenheter>(underenheterResponse, orgnrOverordnetEnhet)

        return data?.let {
            it._embedded?.underenheter?.map {
                VirksomhetDto(
                    organisasjonsnummer = it.organisasjonsnummer,
                    navn = it.navn,
                    overordnetEnhet = orgnrOverordnetEnhet,
                    underenheter = null,
                    poststed = it.beliggenhetsadresse?.poststed,
                    postnummer = it.beliggenhetsadresse?.postnummer,
                )
            }
        } ?: emptyList()
    }

    private suspend inline fun <reified T> tolkRespons(response: HttpResponse, orgnr: String): T? {
        return when (response.status) {
            HttpStatusCode.OK -> response.body<T>()
            HttpStatusCode.NotFound -> {
                log.debug("Fant ikke enhet i Brreg med organisasjonsnummer: $orgnr")
                null
            }

            else -> throw ResponseException(response, "Unexpected response from Brreg sitt Api")
        }
    }

    private fun validerOrgnr(orgnr: String) {
        if (!OrgnummerUtil.erOrgnr(orgnr)) {
            throw BadRequestException("Orgnr må være 9 siffer. Oppgitt orgnr: '$orgnr'")
        }
    }
}

@Serializable
internal data class BrregEnhet(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null,
    val beliggenhetsadresse: Adresse? = null,
    val slettedato: String? = null,
)

@Serializable
internal data class BrregEmbeddedHovedenheter(
    val _embedded: BrregHovedenheter? = null,
)

@Serializable
internal data class BrregHovedenheter(
    val enheter: List<BrregEnhet>,
)

@Serializable
internal data class BrregEmbeddedUnderenheter(
    val _embedded: BrregUnderenheter? = null,
)

@Serializable
internal data class BrregUnderenheter(
    val underenheter: List<BrregEnhet>,
)

@Serializable
internal data class Adresse(
    val poststed: String? = null,
    val postnummer: String? = null,
)
