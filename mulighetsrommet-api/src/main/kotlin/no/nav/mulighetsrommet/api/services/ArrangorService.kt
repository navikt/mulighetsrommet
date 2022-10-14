package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.clients.amtenhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.clients.arenaordsproxy.ArenaOrdsProxyClient
import java.util.concurrent.TimeUnit

class ArrangorService(
    private val arenaOrdsProxyClient: ArenaOrdsProxyClient,
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient
) {

    val arrangorCache: Cache<Int, String> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .build()

    suspend fun hentArrangorNavn(arrangorId: Int): String? {
        val cahchedArrangorNavn = arrangorCache.getIfPresent(arrangorId)

        if (cahchedArrangorNavn != null) return cahchedArrangorNavn

        val arbeidsgiverInfo = arenaOrdsProxyClient.hentArbeidsgiver(arrangorId)
        val virksomhetInfo =
            arbeidsgiverInfo?.virksomhetsnummer?.let { amtEnhetsregisterClient.hentVirksomhetsNavn(it.toInt()) }

        val arrangorNavn = virksomhetInfo?.overordnetEnhetNavn
        arrangorNavn.let { arrangorCache.put(arrangorId, it) }

        return arrangorNavn
    }
}
