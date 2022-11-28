package no.nav.mulighetsrommet.api.clients.veileder

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import no.nav.mulighetsrommet.api.domain.VeilederDTO
import no.nav.mulighetsrommet.api.setup.http.httpJsonClient
import no.nav.poao_tilgang.client.utils.CacheUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger(VeilarbveilederClientImpl::class.java)

private val cache: Cache<String, VeilederDTO> = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .maximumSize(10_000)
    .build()

class VeilarbveilederClientImpl(
    private val baseUrl: String,
    private val veilarbVeilederTokenProvider: (accessToken: String) -> String,
    clientEngine: HttpClientEngine = CIO.create()
) : VeilarbveilederClient {
    val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }
    override suspend fun hentVeilederdata(accessToken: String, navAnsattAzureId: UUID): VeilederDTO? {
        return CacheUtils.tryCacheFirstNotNull(cache, navAnsattAzureId.toString()) {
            return try {
                client.get("$baseUrl/veileder/me") {
                    bearerAuth(
                        veilarbVeilederTokenProvider.invoke(accessToken)
                    )
                }.body<VeilederDTO>()
            } catch (exe: Exception) {
                log.error("Klarte ikke hente data om veileder")
                null
            }
        }
    }
}
