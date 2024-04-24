package no.nav.mulighetsrommet.api.clients.norg2

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.pdl.GeografiskTilknytning
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class Norg2Client(
    private val baseUrl: String,
    clientEngine: HttpClientEngine = CIO.create(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
        install(HttpRequestRetry) {
            retryOnException(maxRetries = 5, retryOnTimeout = true)
            exponentialDelay()
        }
        install(HttpTimeout)
    }

    private val hentEnhetByGeografiskOmraadeCache: Cache<GeografiskTilknytning, Norg2EnhetDto> = Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun hentEnheter(): List<Norg2Response> {
        return try {
            val response = client.get("$baseUrl/enhet/kontaktinformasjon/organisering/all") {
                headers {
                    this.append("consumerId", "team-mulighetsrommet-enhet-sync")
                }
            }
            response.body()
        } catch (exe: Exception) {
            log.error("Klarte ikke hente enheter fra NORG2. Konsekvensen er at oppdatering av enheter mot database ikke blir kjørt")
            throw exe
        }
    }

    suspend fun hentEnhetByGeografiskOmraade(geografiskTilknytning: GeografiskTilknytning): Norg2EnhetDto? {
        hentEnhetByGeografiskOmraadeCache.getIfPresent(geografiskTilknytning)?.let { return@hentEnhetByGeografiskOmraade it }

        val geografiskOmraade = when (geografiskTilknytning) {
            is GeografiskTilknytning.GtBydel -> geografiskTilknytning.value
            is GeografiskTilknytning.GtKommune -> geografiskTilknytning.value
            is GeografiskTilknytning.GtUtland, GeografiskTilknytning.GtUdefinert -> return null
        }

        return try {
            val response = client.get("$baseUrl/enhet/navkontor/$geografiskOmraade") {
                headers {
                    this.append("consumerId", "team-mulighetsrommet")
                }
            }
            when (response.status) {
                HttpStatusCode.NotFound -> {
                    log.error("Fant ikke Nav enhet for geografisk område: $geografiskTilknytning")
                    null
                }
                HttpStatusCode.OK -> {
                    val enhet = response.body<Norg2EnhetDto>()
                    hentEnhetByGeografiskOmraadeCache.put(geografiskTilknytning, enhet)
                    enhet
                }
                else -> {
                    throw Exception("Error fra Norg: $response")
                }
            }
        } catch (exe: Exception) {
            log.error("Klarte ikke hente enhet basert på geografisk tilknytning fra NORG2. geografiskOmraade: $geografiskTilknytning", exe)
            throw exe
        }
    }
}
