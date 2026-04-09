package no.nav.mulighetsrommet.brreg

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
        return getUnderenhet(orgnr).fold(
            { error ->
                if (error == BrregError.NotFound) {
                    getHovedenhet(orgnr)
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
                        overordnetEnhet = it.overordnetEnhet,
                        postadresse = it.postadresse?.toBrregAdresse(),
                        forretningsadresse = it.forretningsadresse?.toBrregAdresse(),
                    )
                } ?: emptyList()
            }
    }

    suspend fun searchUnderenhet(search: String): Either<BrregError, List<BrregUnderenhet>> {
        return when (val orgnr = Organisasjonsnummer.parse(search)) {
            null -> {
                val response = client.get("/enhetsregisteret/api/underenheter") {
                    parameter("size", 20)
                    parameter("navn", search)
                }
                parseResponse<EmbeddedUnderenheter>(response).map { data ->
                    data._embedded?.underenheter?.map { toBrregUnderenhet(it) } ?: emptyList()
                }
            }

            else -> getUnderenhet(orgnr).fold(
                { error ->
                    if (error is BrregError.NotFound) {
                        getUnderenheterForHovedenhet(orgnr)
                    } else {
                        error.left()
                    }
                },
                { underenhet -> listOf(underenhet).right() },
            )
        }
    }

    suspend fun getUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<BrregError, List<BrregUnderenhet>> {
        val underenheterResponse = client.get("/enhetsregisteret/api/underenheter") {
            parameter("size", 1000)
            parameter("overordnetEnhet", orgnr.value)
        }

        return parseResponse<EmbeddedUnderenheter>(underenheterResponse).map { data ->
            data._embedded?.underenheter?.map { toBrregUnderenhet(it) } ?: emptyList()
        }
    }

    suspend fun getHovedenhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregHovedenhet> {
        val response = client.get("/enhetsregisteret/api/enheter/${orgnr.value}")

        return parseResponse<OverordnetEnhet>(response).map { toBrregHovedenhet(it) }
    }

    suspend fun getUnderenhet(orgnr: Organisasjonsnummer): Either<BrregError, BrregUnderenhet> {
        val response = client.get("/enhetsregisteret/api/underenheter/${orgnr.value}")

        return parseResponse<Underenhet>(response).map { toBrregUnderenhet(it) }
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

private fun toBrregHovedenhet(enhet: OverordnetEnhet): BrregHovedenhet = if (enhet.slettedato != null) {
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
        overordnetEnhet = enhet.overordnetEnhet,
        postadresse = enhet.postadresse?.toBrregAdresse(),
        forretningsadresse = enhet.forretningsadresse?.toBrregAdresse(),
    )
}

private fun toBrregUnderenhet(enhet: Underenhet): BrregUnderenhet = when {
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
