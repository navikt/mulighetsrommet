package no.nav.mulighetsrommet.api.clients.isoppfolgingstilfelle

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.body
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.TokenProvider
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class IsoppfolgingstilfelleClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val personIdentHeader = "NAV_PERSONIDENT_HEADER"

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    private val erSykmeldtMedArbeidsgiverCache: Cache<NorskIdent, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun erSykmeldtMedArbeidsgiver(
        norskIdent: NorskIdent,
        obo: AccessType.OBO,
    ): Either<OppfolgingstilfelleError, Boolean> {
        erSykmeldtMedArbeidsgiverCache.getIfPresent(norskIdent)?.let { return@erSykmeldtMedArbeidsgiver it.right() }

        return hentOppfolgingstilfeller(norskIdent, obo)
            .map { oppfolgingstilfeller ->
                oppfolgingstilfeller
                    .filter { it.gyldigForDato(LocalDate.now()) }
                    .firstOrNull { it.arbeidstakerAtTilfelleEnd } != null
            }
    }

    private suspend fun hentOppfolgingstilfeller(norskIdent: NorskIdent, obo: AccessType.OBO): Either<OppfolgingstilfelleError, List<OppfolgingstilfelleDTO>> {
        val response = client.get("$baseUrl/api/internad/v1/oppfolgingstilfelle/personident") {
            bearerAuth(tokenProvider.exchange(obo))
            header(personIdentHeader, norskIdent)
            contentType(ContentType.Application.Json)
        }

        return when (response.status) {
            HttpStatusCode.Forbidden -> {
                log.info("Manglet SYFO rolle for å hente oppfølgingstilfeller for bruker.")
                OppfolgingstilfelleError.Forbidden.left()
            }

            else -> if (!response.status.isSuccess()) {
                log.error(
                    "Kunne ikke hente oppfølgingstilfelle fra isoppfolgingstilfelle. " +
                        "Status=${response.status.value} error=${response.bodyAsText()}",
                )
                OppfolgingstilfelleError.Error.left()
            } else {
                response.body<OppfolgingstilfellePersonDTO>()
                    .oppfolgingstilfelleList
                    .right()
            }
        }
    }
}

enum class OppfolgingstilfelleError {
    Forbidden,
    Error,
}

@Serializable
data class OppfolgingstilfellePersonDTO(
    val oppfolgingstilfelleList: List<OppfolgingstilfelleDTO>,
)

@Serializable
data class OppfolgingstilfelleDTO(
    val arbeidstakerAtTilfelleEnd: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val end: LocalDate,
) {
    fun gyldigForDato(dato: LocalDate): Boolean {
        return dato in start..end
    }
}
