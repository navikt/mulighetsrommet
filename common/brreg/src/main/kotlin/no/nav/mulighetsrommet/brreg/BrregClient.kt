package no.nav.mulighetsrommet.brreg

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Klient for å hente data fra Brreg.
 *
 * Se [0] for dokumentasjon til API, i tillegg kan [1] være til hjelp for bedre forståelse av tilgjengelige data.
 *
 * [0]: https://data.brreg.no/enhetsregisteret/api/docs/index.html for dokumentasjon.
 * [1]: https://www.brreg.no/bruke-data-fra-bronnoysundregistrene/apne-data/beskrivelse-av-tjenesten-data-fra-enhetsregisteret/
 */
class BrregClient(clientEngine: HttpClientEngine, private val baseUrl: String) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun getBrregEnhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregEnhet> {
        // Sjekker først hovedenhet
        return getHovedenhet(orgnr).fold(
            { error ->
                if (error == BrregError.NotFound) {
                    // Ingen treff på hovedenhet, vi sjekker underenheter også
                    log.debug("Fant ingen treff på orgnr: '$orgnr'. Sjekker underenheter....")
                    getUnderenhet(orgnr)
                } else {
                    error.left()
                }
            },
            {
                it.right()
            },
        )
    }

    suspend fun sokHovedenhet(orgnr: String): Either<BrregError, List<BrregHovedenhetDto>> {
        val sokEllerOppslag = when (Organisasjonsnummer.isValid(orgnr)) {
            true -> "organisasjonsnummer"
            false -> "navn"
        }

        val response = client.get("$baseUrl/enheter") {
            parameter("size", 20)
            parameter(sokEllerOppslag, orgnr)
        }

        return parseResponse<EmbeddedEnheter>(response)
            .map { data ->
                data._embedded?.enheter?.map {
                    BrregHovedenhetDto(
                        organisasjonsnummer = it.organisasjonsnummer,
                        organisasjonsform = it.organisasjonsform.kode,
                        navn = it.navn,
                        postnummer = it.postAdresse?.postnummer,
                        poststed = it.postAdresse?.poststed,
                    )
                } ?: emptyList()
            }
    }

    suspend fun getUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<BrregError, List<BrregUnderenhetDto>> {
        val underenheterResponse = client.get("$baseUrl/underenheter") {
            parameter("size", 1000)
            parameter("overordnetEnhet", orgnr.value)
        }

        return parseResponse<EmbeddedUnderenheter>(underenheterResponse)
            .map { data ->
                data._embedded?.underenheter?.map { underenhet ->
                    BrregUnderenhetDto(
                        organisasjonsnummer = underenhet.organisasjonsnummer,
                        organisasjonsform = underenhet.organisasjonsform.kode,
                        navn = underenhet.navn,
                        overordnetEnhet = orgnr,
                        poststed = underenhet.beliggenhetsadresse?.poststed,
                        postnummer = underenhet.beliggenhetsadresse?.postnummer,
                    )
                } ?: emptyList()
            }
    }

    suspend fun getHovedenhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregHovedenhet> = either {
        val response = client.get("$baseUrl/enheter/${orgnr.value}")

        val enhet = parseResponse<OverordnetEnhet>(response).bind()
        if (enhet.slettedato != null) {
            logSlettetWarning(orgnr, enhet.slettedato)
            SlettetBrregHovedenhetDto(
                organisasjonsnummer = enhet.organisasjonsnummer,
                organisasjonsform = enhet.organisasjonsform.kode,
                navn = enhet.navn,
                slettetDato = enhet.slettedato,
            )
        } else {
            BrregHovedenhetDto(
                organisasjonsnummer = enhet.organisasjonsnummer,
                organisasjonsform = enhet.organisasjonsform.kode,
                navn = enhet.navn,
                postnummer = enhet.postAdresse?.postnummer,
                poststed = enhet.postAdresse?.poststed,
            )
        }
    }

    suspend fun getUnderenhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregUnderenhet> = either {
        val response = client.get("$baseUrl/underenheter/${orgnr.value}")

        val enhet = parseResponse<Underenhet>(response).bind()

        when {
            enhet.slettedato != null -> {
                logSlettetWarning(orgnr, enhet.slettedato)
                SlettetBrregUnderenhetDto(
                    organisasjonsnummer = enhet.organisasjonsnummer,
                    organisasjonsform = enhet.organisasjonsform.kode,
                    navn = enhet.navn,
                    slettetDato = enhet.slettedato,
                )
            }

            enhet.overordnetEnhet == null -> {
                throw IllegalStateException("Fant underenhet uten overordnet enhet bra brreg: ${enhet.organisasjonsnummer}")
            }

            else -> {
                BrregUnderenhetDto(
                    organisasjonsnummer = enhet.organisasjonsnummer,
                    organisasjonsform = enhet.organisasjonsform.kode,
                    navn = enhet.navn,
                    overordnetEnhet = enhet.overordnetEnhet,
                    postnummer = enhet.beliggenhetsadresse?.postnummer,
                    poststed = enhet.beliggenhetsadresse?.poststed,
                )
            }
        }
    }

    private fun logSlettetWarning(organisasjonsnummer: Organisasjonsnummer, localDate: LocalDate) {
        log.warn("Enhet med orgnr: $organisasjonsnummer er slettet fra Brreg. Slettedato: $localDate.")
    }

    private suspend inline fun <reified T> parseResponse(response: HttpResponse): Either<BrregError, T> {
        return when (response.status) {
            HttpStatusCode.OK -> response.body<T>().right()

            HttpStatusCode.BadRequest -> {
                val bodyAsText = response.bodyAsText()
                log.warn("BadRequest: response=$bodyAsText")
                BrregError.BadRequest.left()
            }

            HttpStatusCode.NotFound -> {
                BrregError.NotFound.left()
            }

            else -> {
                val bodyAsText = response.bodyAsText()
                log.error("Error: response=$bodyAsText")
                BrregError.Error.left()
            }
        }
    }
}

enum class BrregError {
    BadRequest,
    NotFound,
    Error,
}
