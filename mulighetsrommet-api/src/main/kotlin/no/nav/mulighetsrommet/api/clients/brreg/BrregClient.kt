package no.nav.mulighetsrommet.api.clients.brreg

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.time.LocalDate

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

    suspend fun sokEtterOverordnetEnheter(orgnr: String): Either<BrregError, List<VirksomhetDto>> {
        val sokEllerOppslag = when (OrgnummerUtil.erOrgnr(orgnr)) {
            true -> "organisasjonsnummer"
            false -> "navn"
        }

        val response = client.get("$baseUrl/enheter") {
            parameter("size", 20)
            parameter("sort", "navn,ASC")
            parameter(sokEllerOppslag, orgnr)
        }

        return parseResponse<BrregEmbeddedHovedenheter>(response, orgnr)
            .map { data ->
                data._embedded?.enheter?.map {
                    VirksomhetDto(
                        organisasjonsnummer = it.organisasjonsnummer,
                        navn = it.navn,
                        postnummer = it.beliggenhetsadresse?.postnummer,
                        poststed = it.beliggenhetsadresse?.poststed,
                    )
                } ?: emptyList()
            }
    }

    suspend fun hentUnderenheterForOverordnetEnhet(orgnr: String): Either<BrregError, List<VirksomhetDto>> {
        val underenheterResponse = client.get("$baseUrl/underenheter") {
            parameter("size", 1000)
            parameter("sort", "navn,ASC")
            parameter("overordnetEnhet", orgnr)
        }

        return parseResponse<BrregEmbeddedUnderenheter>(underenheterResponse, orgnr)
            .map { data ->
                data._embedded?.underenheter?.map { underenhet ->
                    VirksomhetDto(
                        organisasjonsnummer = underenhet.organisasjonsnummer,
                        navn = underenhet.navn,
                        overordnetEnhet = orgnr,
                        underenheter = null,
                        poststed = underenhet.beliggenhetsadresse?.poststed,
                        postnummer = underenhet.beliggenhetsadresse?.postnummer,
                    )
                } ?: emptyList()
            }
    }

    suspend fun getHovedenhet(orgnr: String): Either<BrregError, VirksomhetDto> {
        val response = client.get("$baseUrl/enheter/$orgnr")
        return parseResponse<BrregEnhet>(response, orgnr)
            .flatMap { enhet ->
                val underenheterResult = if (enhet.slettedato == null) {
                    hentUnderenheterForOverordnetEnhet(orgnr)
                } else {
                    log.info("Enhet med orgnr: $orgnr er slettet fra Brreg. Slettedato: ${enhet.slettedato}.")
                    emptyList<VirksomhetDto>().right()
                }

                underenheterResult.map { underenheter ->
                    VirksomhetDto(
                        organisasjonsnummer = enhet.organisasjonsnummer,
                        navn = enhet.navn,
                        overordnetEnhet = null,
                        underenheter = underenheter,
                        slettetDato = enhet.slettedato,
                        postnummer = enhet.beliggenhetsadresse?.postnummer,
                        poststed = enhet.beliggenhetsadresse?.poststed,
                    )
                }
            }
    }

    suspend fun getUnderenhet(orgnr: String): Either<BrregError, VirksomhetDto> {
        val response = client.get("$baseUrl/underenheter/$orgnr")
        return parseResponse<BrregEnhet>(response, orgnr)
            .map { enhet ->
                val hovedenhet = if (enhet.slettedato == null) {
                    enhet.overordnetEnhet
                } else {
                    log.info("Enhet med orgnr: $orgnr er slettet fra Brreg. Slettedato: ${enhet.slettedato}.")
                    null
                }

                VirksomhetDto(
                    organisasjonsnummer = enhet.organisasjonsnummer,
                    navn = enhet.navn,
                    overordnetEnhet = hovedenhet,
                    underenheter = null,
                    slettetDato = enhet.slettedato,
                    postnummer = enhet.beliggenhetsadresse?.postnummer,
                    poststed = enhet.beliggenhetsadresse?.poststed,
                )
            }
    }

    private suspend inline fun <reified T> parseResponse(response: HttpResponse, orgnr: String): Either<BrregError, T> {
        return when (response.status) {
            HttpStatusCode.OK -> response.body<T>().right()

            HttpStatusCode.BadRequest -> {
                val bodyAsText = response.bodyAsText()
                log.warn("BadRequest: orgnr=$orgnr response=$bodyAsText")
                BrregError.BadRequest.left()
            }

            HttpStatusCode.NotFound -> {
                log.warn("NotFound: orgnr=$orgnr")
                BrregError.NotFound.left()
            }

            else -> {
                val bodyAsText = response.bodyAsText()
                log.error("Error: orgnr=$orgnr response=$bodyAsText")
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

@Serializable
internal data class BrregEnhet(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String? = null,
    val beliggenhetsadresse: Adresse? = null,
    @Serializable(with = LocalDateSerializer::class)
    val slettedato: LocalDate? = null,
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
