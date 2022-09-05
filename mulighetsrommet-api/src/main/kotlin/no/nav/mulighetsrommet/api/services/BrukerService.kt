package no.nav.mulighetsrommet.api.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.oppfolging.VeilarboppfolgingClient
import no.nav.mulighetsrommet.api.clients.person.VeilarbpersonClient
import no.nav.mulighetsrommet.api.clients.vedtak.VeilarbvedtaksstotteClient
import no.nav.mulighetsrommet.api.domain.Innsatsgruppe
import no.nav.mulighetsrommet.api.domain.ManuellStatusDTO
import no.nav.mulighetsrommet.api.domain.Oppfolgingsenhet
import java.util.concurrent.TimeUnit

class BrukerService(
    private val veilarboppfolgingClient: VeilarboppfolgingClient,
    private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
    private val veilarbpersonClient: VeilarbpersonClient
) {

    val brukerCache: Cache<String, Brukerdata> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(500)
        .build()

    suspend fun hentBrukerdata(fnr: String, accessToken: String?, callId: String?): Brukerdata {
        val cachedBrukerdata = brukerCache.getIfPresent(fnr)

        if (cachedBrukerdata != null) return cachedBrukerdata

        val oppfolgingsenhet = veilarboppfolgingClient.hentOppfolgingsstatus(fnr, accessToken, callId)
        val manuellStatus = veilarboppfolgingClient.hentManuellStatus(fnr, accessToken, callId)
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, accessToken, callId)
        val personInfo = veilarbpersonClient.hentPersonInfo(fnr, accessToken, callId)

        val brukerdata = Brukerdata(
            fnr = fnr,
            oppfolgingsenhet = oppfolgingsenhet?.oppfolgingsenhet,
            innsatsgruppe = sisteVedtak?.innsatsgruppe,
            fornavn = personInfo?.fornavn,
            manuellStatus = manuellStatus
        )
        brukerCache.put(fnr, brukerdata)
        return brukerdata
    }
}

@Serializable
data class Brukerdata(
    val fnr: String,
    val innsatsgruppe: Innsatsgruppe?,
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val fornavn: String?,
    val manuellStatus: ManuellStatusDTO?
)
