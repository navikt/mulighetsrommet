package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.mulighetsrommet.api.clients.arena_ords_proxy.ArenaOrdsProxyClient
import no.nav.mulighetsrommet.api.clients.enhetsregister.AmtEnhetsregisterClient
import java.util.concurrent.TimeUnit

class ArrangorService(
    private val arenaOrdsProxyClient: ArenaOrdsProxyClient,
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient
) {

    val arrangorCache: Cache<Int, String> = Caffeine.newBuilder()
        .expireAfterWrite(12, TimeUnit.HOURS)
        .maximumSize(10000)
        .build()

    suspend fun hentArrangornavn(arrangorId: Int): String? {
        val cahchedArrangornavn = arrangorCache.getIfPresent(arrangorId)

        if (cahchedArrangornavn != null) return cahchedArrangornavn

        val arrangor = arenaOrdsProxyClient.hentArbeidsgiver(arrangorId)
        val virksomhet =
            arrangor?.virksomhetsnummer?.let { amtEnhetsregisterClient.hentVirksomhet(it.toInt()) }

        val arrangornavn = virksomhet?.overordnetEnhetNavn
        arrangornavn.let { arrangorCache.put(arrangorId, it) }

        return arrangornavn
    }
}
