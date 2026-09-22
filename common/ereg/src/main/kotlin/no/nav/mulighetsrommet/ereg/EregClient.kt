package no.nav.mulighetsrommet.ereg

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.slf4j.LoggerFactory

/**
 * Klient for å hente data fra Ereg, Nav sitt register over enheter og virksomheter.
 *
 * Se https://github.com/navikt/ereg-services for dokumentasjon av API-et.
 */
class EregClient(
    clientEngine: HttpClientEngine,
    private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    /**
     * Henter enheten som er registrert på [orgnr] direkte, enten det er en juridisk enhet,
     * et organisasjonsledd eller en virksomhet/underenhet - uten noen oppslag til over-/underenheter.
     */
    suspend fun getEnhet(orgnr: Organisasjonsnummer): Either<EregError, EregEnhet> {
        return getOrganisasjon(orgnr).map { it.toEregEnhet() }
    }

    /**
     * Henter juridisk enhet (hovedenhet) for [orgnr]. Dersom [orgnr] tilhører en underliggende
     * virksomhet, slås den juridiske enheten den til slutt inngår i opp automatisk - eventuelle
     * organisasjonsledd i kjeden mellom virksomheten og den juridiske enheten hoppes over, i tråd
     * med hvordan Brreg sin underenhet.overordnetEnhet alltid peker direkte på hovedenheten.
     */
    suspend fun getHovedenhet(orgnr: Organisasjonsnummer): Either<EregError, EregHovedenhet> {
        return getOrganisasjon(orgnr).flatMap { organisasjon ->
            if (organisasjon is Virksomhet) {
                val juridiskEnhet = organisasjon.finnJuridiskEnhet()

                if (juridiskEnhet == null) {
                    val message = "Fant ikke juridisk enhet for virksomhet med orgnr ${orgnr.value} i Ereg"
                    log.error(message)
                    EregError.Error(message).left()
                } else {
                    getOrganisasjon(juridiskEnhet).map { it.toEregHovedenhet() }
                }
            } else {
                organisasjon.toEregHovedenhet().right()
            }
        }
    }

    /**
     * Henter virksomhet/underenhet for [orgnr]. Returnerer [EregError.NotFound] dersom [orgnr]
     * peker på en juridisk enhet eller et organisasjonsledd i stedet for en virksomhet.
     */
    suspend fun getUnderenhet(orgnr: Organisasjonsnummer): Either<EregError, EregUnderenhet> {
        return getOrganisasjon(orgnr).flatMap { organisasjon ->
            if (organisasjon is Virksomhet) {
                organisasjon.toEregUnderenhet().right()
            } else {
                log.warn("Orgnr ${orgnr.value} er ikke en virksomhet i Ereg (type=${organisasjon::class.simpleName})")
                EregError.NotFound.left()
            }
        }
    }

    /**
     * Henter virksomhetene til den juridiske enheten [orgnr].
     */
    suspend fun getUnderenheterForHovedenhet(orgnr: Organisasjonsnummer): Either<EregError, List<EregUnderenhetDto>> {
        return getOrganisasjon(orgnr).map { organisasjon ->
            val driverVirksomheter = when (organisasjon) {
                is JuridiskEnhet -> organisasjon.driverVirksomheter
                is Organisasjonsledd -> organisasjon.driverVirksomheter
                is Virksomhet -> emptyList()
            }
            driverVirksomheter.map { virksomhet ->
                EregUnderenhetDto(
                    organisasjonsnummer = virksomhet.organisasjonsnummer,
                    organisasjonsform = null,
                    navn = virksomhet.navn.tilNavnString(virksomhet.organisasjonsnummer),
                    overordnetEnhet = orgnr,
                )
            }
        }
    }

    /**
     * Søk etter juridiske enheter/organisasjonsledd på navn eller orgnr.
     */
    suspend fun searchHovedenhet(search: String): Either<EregError, List<EregHovedenhetDto>> {
        return when (val orgnr = Organisasjonsnummer.parse(search)) {
            null -> finnOrganisasjon(search).map { treff ->
                treff.filter { !it.erVirksomhet() }.map { it.toEregHovedenhetDto() }
            }

            else -> getHovedenhet(orgnr).fold(
                { error -> if (error == EregError.NotFound) emptyList<EregHovedenhetDto>().right() else error.left() },
                { hovedenhet -> listOfNotNull(hovedenhet as? EregHovedenhetDto).right() },
            )
        }
    }

    /**
     * Søk etter virksomheter/underenheter på navn eller orgnr. Se [searchHovedenhet] for begrensninger.
     */
    suspend fun searchUnderenhet(search: String): Either<EregError, List<EregUnderenhetDto>> {
        return when (val orgnr = Organisasjonsnummer.parse(search)) {
            null -> finnOrganisasjon(search).map { treff ->
                treff.filter { it.erVirksomhet() }.map { it.toEregUnderenhetDto() }
            }

            else -> getUnderenhet(orgnr).fold(
                { error -> if (error == EregError.NotFound) emptyList<EregUnderenhetDto>().right() else error.left() },
                { underenhet -> listOfNotNull(underenhet as? EregUnderenhetDto).right() },
            )
        }
    }

    private suspend fun getOrganisasjon(orgnr: Organisasjonsnummer): Either<EregError, Organisasjon> {
        val response = client.get("$baseUrl/v2/organisasjon/${orgnr.value}") {
            // Gjør at `inngaarIJuridiskEnheter`/`bestaarAvOrganisasjonsledd`/`driverVirksomheter` inngår i responsen
            parameter("inkluderHierarki", true)
            parameter("inkluderHistorikk", false)
        }

        return parseResponse(response)
    }

    private suspend fun finnOrganisasjon(navn: String): Either<EregError, List<OrganisasjonSammendrag>> {
        val response = client.get("$baseUrl/v2/organisasjon/finn") {
            parameter("organisasjonsnavn", navn)
            parameter("antall", 10)
            parameter("opphoert", false)
        }

        return parseResponse<OrganisasjonSammendragResultat>(response).map { it.organisasjonSammendrag }
    }

    private suspend inline fun <reified T> parseResponse(response: HttpResponse): Either<EregError, T> {
        return when (response.status) {
            HttpStatusCode.OK -> response.body<T>().right()

            HttpStatusCode.BadRequest -> {
                val bodyAsText = response.bodyAsText()
                log.warn("BadRequest: response=$bodyAsText")
                EregError.BadRequest.left()
            }

            HttpStatusCode.NotFound -> EregError.NotFound.left()

            else -> {
                val bodyAsText = response.bodyAsText()
                val message = "Uventet feil fra Ereg: status=${response.status}, response=$bodyAsText"
                log.error(message)
                EregError.Error(message).left()
            }
        }
    }
}

private fun Organisasjon.toEregEnhet(): EregEnhet = when (this) {
    is Virksomhet -> toEregUnderenhet()
    is JuridiskEnhet, is Organisasjonsledd -> toEregHovedenhet()
}

private fun Organisasjon.toEregHovedenhet(): EregHovedenhet {
    val navnString = navn.tilNavnString(organisasjonsnummer)
    val organisasjonsform = organisasjonDetaljer?.enhetstyper?.tilOrganisasjonsform()
    val opphoersdato = organisasjonDetaljer?.opphoersdato

    val overordnetEnhet = when (this) {
        is Organisasjonsledd -> finnDirekteOverordnetEnhet()
        is JuridiskEnhet, is Virksomhet -> null
    }

    return if (opphoersdato != null) {
        SlettetEregHovedenhetDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navnString,
            slettetDato = opphoersdato,
        )
    } else {
        EregHovedenhetDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navnString,
            postadresse = organisasjonDetaljer?.postadresser?.firstOrNull()?.toEregAdresse(),
            forretningsadresse = organisasjonDetaljer?.forretningsadresser?.firstOrNull()?.toEregAdresse(),
            overordnetEnhet = overordnetEnhet,
        )
    }
}

private fun Virksomhet.toEregUnderenhet(): EregUnderenhet {
    val navnString = navn.tilNavnString(organisasjonsnummer)
    val organisasjonsform = organisasjonDetaljer?.enhetstyper?.tilOrganisasjonsform()
    val opphoersdato = organisasjonDetaljer?.opphoersdato

    return if (opphoersdato != null) {
        SlettetEregUnderenhetDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navnString,
            slettetDato = opphoersdato,
        )
    } else {
        EregUnderenhetDto(
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navnString,
            overordnetEnhet = finnJuridiskEnhet(),
        )
    }
}

private fun OrganisasjonSammendrag.toEregHovedenhetDto(): EregHovedenhetDto = EregHovedenhetDto(
    organisasjonsnummer = organisasjonsnummer,
    organisasjonsform = enhetstype,
    navn = tilNavnString(),
    postadresse = null,
    forretningsadresse = tilEregAdresse(),
    overordnetEnhet = null,
)

private fun OrganisasjonSammendrag.toEregUnderenhetDto(): EregUnderenhetDto = EregUnderenhetDto(
    organisasjonsnummer = organisasjonsnummer,
    organisasjonsform = enhetstype,
    navn = tilNavnString(),
    overordnetEnhet = juridiskEnhetOrganisasjonsnummer,
)
