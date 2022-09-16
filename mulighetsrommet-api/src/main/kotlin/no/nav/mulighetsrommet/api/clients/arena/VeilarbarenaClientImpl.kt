package no.nav.mulighetsrommet.api.clients.arena

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.common.token_client.client.AzureAdOnBehalfOfTokenClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.mulighetsrommet.api.setup.http.baseClient
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger(VeilarbarenaClientImpl::class.java)
private val secureLog = SecureLog.logger

class VeilarbarenaClientImpl(
    private val baseUrl: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val oboTokenClient: AzureAdOnBehalfOfTokenClient,
    private val scope: String,
    private val proxyScope: String,
    private val client: HttpClient = baseClient.config {
        install(HttpCache)
    }
) : VeilarbarenaClient {

    val fnrTilpersonIdCache: Cache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .maximumSize(500)
        .build()

    override suspend fun hentPersonIdForFnr(fnr: String, accessToken: String?): String? {
        val cachedPersonIdForFnr = fnrTilpersonIdCache.getIfPresent(fnr)

        if (cachedPersonIdForFnr != null) return cachedPersonIdForFnr

        return try {
            val response = client.get("$baseUrl/proxy/veilarbarena/api/oppfolgingsbruker/hentPersonId") {
                bearerAuth(
                    machineToMachineTokenClient.createMachineToMachineToken(
                        proxyScope
                    )
                )
                headers {
                    append(
                        "Downstream-Authorization",
                        "Bearer ${oboTokenClient.exchangeOnBehalfOfToken(scope, accessToken)}"
                    )
                    append("Nav-Consumer-Id", "mulighetsrommet-api")
                }
                parameter("fnr", fnr)
            }

            if (response.status == HttpStatusCode.OK) {
                val personId = response.bodyAsText()
                fnrTilpersonIdCache.put(fnr, personId)
                return personId
            }

            if (response.status == HttpStatusCode.NoContent) {
                secureLog.info("Det ble ikke returnert personId fra veilarbarena for bruker med fnr: $fnr")
                log.info("Det ble ikke returnert personId fra veilarbarena. Det kan være fordi bruker ikke er under oppfølging eller ikke finnes i Arena")
            }

            if (response.status == HttpStatusCode.NotFound) {
                secureLog.info("Fant ikke personId fra veilarbarena for bruker med fnr: $fnr")
                log.info("Fant ikke personId. Det kan være feil endepunkt til veilarbarena, eller at personId ikke finnes for innsendt fnr.")
            }
            null
        } catch (exe: Exception) {
            secureLog.error("Klarte ikke hente personId for bruker med fnr: $fnr", exe)
            log.error("Klarte ikke hente personId for bruker. Se detaljer i secureLog.")
            null
        }
    }
}
