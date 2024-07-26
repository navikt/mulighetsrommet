package no.nav.mulighetsrommet.api.clients.oppfolging

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.TokenProvider
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.domain.serializers.ZonedDateTimeSerializer
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class VeilarboppfolgingClient(
    private val baseUrl: String,
    private val tokenProvider: TokenProvider,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 3, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }

    private val oppfolgingsenhetCache: Cache<NorskIdent, Oppfolgingsenhet> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    private val gjeldendePeriodeCache: Cache<NorskIdent, OppfolgingPeriodeMinimalDTO> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    private val manuellStatusCache: Cache<NorskIdent, ManuellStatusDto> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun hentOppfolgingsenhet(fnr: NorskIdent, obo: AccessType.OBO): Either<OppfolgingError, Oppfolgingsenhet> {
        oppfolgingsenhetCache.getIfPresent(fnr)?.let { return@hentOppfolgingsenhet it.right() }

        val response = client.post("$baseUrl/v2/person/hent-oppfolgingsstatus") {
            bearerAuth(tokenProvider.exchange(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(HentOppfolgingsstatusRequest(fnr = fnr.value))
        }

        return when (response.status) {
            HttpStatusCode.NotFound -> {
                log.info("Fant ikke oppfølgingsstatus for bruker. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
                OppfolgingError.NotFound.left()
            }

            HttpStatusCode.Forbidden -> {
                log.info("Manglet tilgang til å hente oppfølgingsstatus for bruker.")
                OppfolgingError.Forbidden.left()
            }

            else -> if (!response.status.isSuccess()) {
                log.warn("Klarte ikke hente oppfølgingsstatus for bruker. Status: ${response.status}")
                OppfolgingError.Error.left()
            } else {
                response.body<OppfolgingEnhetMedVeilederResponse>().oppfolgingsenhet.right()
            }
                .onRight { oppfolgingsenhetCache.put(fnr, it) }
        }
    }

    suspend fun erBrukerUnderOppfolging(
        fnr: NorskIdent,
        accessType: AccessType,
    ): Either<ErUnderOppfolgingError, Boolean> {
        return hentGjeldendePeriode(fnr, accessType)
            .map { (it.sluttDato == null).right() }
            .getOrElse {
                when (it) {
                    OppfolgingError.Forbidden -> ErUnderOppfolgingError.Forbidden.left()
                    OppfolgingError.Error -> ErUnderOppfolgingError.Error.left()
                    OppfolgingError.NotFound -> false.right()
                }
            }
    }

    private suspend fun hentGjeldendePeriode(
        fnr: NorskIdent,
        accessType: AccessType,
    ): Either<OppfolgingError, OppfolgingPeriodeMinimalDTO> {
        gjeldendePeriodeCache.getIfPresent(fnr)?.let { return@hentGjeldendePeriode it.right() }

        val response = client.post("$baseUrl/v3/oppfolging/hent-gjeldende-periode") {
            bearerAuth(tokenProvider.exchange(accessType))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(HentOppfolgingsstatusRequest(fnr = fnr.value))
        }

        return when (response.status) {
            HttpStatusCode.NotFound -> {
                log.warn("Fikk not found i hentGjeldendePeriode. Trodde ikke dette kunne skje")
                OppfolgingError.NotFound.left()
            }

            HttpStatusCode.Forbidden -> {
                log.info("Manglet tilgang til å hente oppfølgingsstatus for bruker.")
                OppfolgingError.Forbidden.left()
            }

            HttpStatusCode.NoContent -> {
                OppfolgingError.NotFound.left()
            }

            else -> if (!response.status.isSuccess()) {
                log.warn("Klarte ikke hente oppfølgingsstatus for bruker. Status: ${response.status}")
                OppfolgingError.Error.left()
            } else {
                response.body<OppfolgingPeriodeMinimalDTO>().right()
            }
                .onRight { gjeldendePeriodeCache.put(fnr, it) }
        }
    }

    suspend fun hentManuellStatus(fnr: NorskIdent, obo: AccessType.OBO): Either<OppfolgingError, ManuellStatusDto> {
        manuellStatusCache.getIfPresent(fnr)?.let { return@hentManuellStatus it.right() }

        val response = client.post("$baseUrl/v3/manuell/hent-status") {
            bearerAuth(tokenProvider.exchange(obo))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ManuellStatusRequest(fnr = fnr.value))
        }

        return if (response.status == HttpStatusCode.Forbidden) {
            log.warn("Manglet tilgang til å hente manuell status for bruker")
            OppfolgingError.Forbidden.left()
        } else {
            if (!response.status.isSuccess()) {
                log.warn("Klarte ikke hente manuell status for bruker. Status: ${response.status}")
                OppfolgingError.Error.left()
            } else {
                response.body<ManuellStatusDto>().right()
            }
                .onRight { manuellStatusCache.put(fnr, it) }
        }
    }
}

enum class ErUnderOppfolgingError {
    Forbidden,
    Error,
}

enum class OppfolgingError {
    Forbidden,
    NotFound,
    Error,
}

@Serializable
data class HentOppfolgingsstatusRequest(
    val fnr: String,
)

@Serializable
data class ManuellStatusRequest(
    val fnr: String,
)

@Serializable
data class OppfolgingEnhetMedVeilederResponse(
    val oppfolgingsenhet: Oppfolgingsenhet,
)

@Serializable
data class Oppfolgingsenhet(
    val navn: String?,
    val enhetId: String,
)

@Serializable
data class OppfolgingPeriodeMinimalDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val startDato: ZonedDateTime,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val sluttDato: ZonedDateTime? = null,
)

@Serializable
data class ManuellStatusDto(
    val erUnderManuellOppfolging: Boolean,
    val krrStatus: KrrStatus,
) {
    @Serializable
    data class KrrStatus(
        val kanVarsles: Boolean,
        val erReservert: Boolean,
    )
}
