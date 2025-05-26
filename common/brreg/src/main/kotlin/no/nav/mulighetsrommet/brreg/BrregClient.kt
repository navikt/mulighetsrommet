package no.nav.mulighetsrommet.brreg

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory

/**
 * Klient for å hente data fra Brreg.
 *
 * Se [0] for dokumentasjon til API, i tillegg kan [1] være til hjelp for bedre forståelse av tilgjengelige data.
 *
 * [0]: https://data.brreg.no/enhetsregisteret/api/docs/index.html for dokumentasjon.
 * [1]: https://www.brreg.no/bruke-data-fra-bronnoysundregistrene/apne-data/beskrivelse-av-tjenesten-data-fra-enhetsregisteret/
 */
class BrregClient(
    clientEngine: HttpClientEngine,
    private val baseUrl: String = "https://data.brreg.no",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        defaultRequest {
            url(baseUrl)
        }
    }

    suspend fun getBrregEnhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregEnhet> {
        // Sjekker først hovedenhet
        return getHovedenhet(orgnr).fold(
            { error ->
                if (error == BrregError.NotFound) {
                    // Ingen treff på hovedenhet, vi sjekker underenheter også
                    log.debug("Fant ingen treff på orgnr: '${orgnr.value}'. Sjekker underenheter....")
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

        val response = client.get("/enhetsregisteret/api/enheter") {
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
                        postadresse = it.postadresse?.toBrregAdresse(),
                        forretningsadresse = it.forretningsadresse?.toBrregAdresse(),
                    )
                } ?: emptyList()
            }
    }

    suspend fun getUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<BrregError, List<BrregUnderenhetDto>> {
        val underenheterResponse = client.get("/enhetsregisteret/api/underenheter") {
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
                    )
                } ?: emptyList()
            }
    }

    suspend fun getHovedenhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregHovedenhet> = either {
        val response = client.get("/enhetsregisteret/api/enheter/${orgnr.value}")

        val enhet = parseResponse<OverordnetEnhet>(response).bind()
        if (enhet.slettedato != null) {
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
                postadresse = enhet.postadresse?.toBrregAdresse(),
                forretningsadresse = enhet.forretningsadresse?.toBrregAdresse(),
            )
        }
    }

    suspend fun getUnderenhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregUnderenhet> = either {
        val response = client.get("/enhetsregisteret/api/underenheter/${orgnr.value}")

        val enhet = parseResponse<Underenhet>(response).bind()

        when {
            enhet.slettedato != null -> {
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
                )
            }
        }
    }

    private suspend inline fun <reified T> parseResponse(response: HttpResponse): Either<BrregError, T> {
        return when (response.status) {
            HttpStatusCode.OK -> response.body<T>().right()

            HttpStatusCode.Gone -> {
                val enhet = response.body<FjernetEnhet>().let {
                    FjernetBrregEnhetDto(it.organisasjonsnummer, it.slettedato)
                }
                BrregError.FjernetAvJuridiskeArsaker(enhet).left()
            }

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
