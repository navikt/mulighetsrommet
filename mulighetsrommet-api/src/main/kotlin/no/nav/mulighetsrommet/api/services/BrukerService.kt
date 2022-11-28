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

    suspend fun hentBrukerdata(fnr: String, accessToken: String?): Brukerdata {
        val cachedBrukerdata = brukerCache.getIfPresent(fnr)

        if (cachedBrukerdata != null) return cachedBrukerdata

        val oppfolgingsstatus = veilarboppfolgingClient.hentOppfolgingsstatus(fnr, accessToken)
        val manuellStatus = veilarboppfolgingClient.hentManuellStatus(fnr, accessToken)
        val sisteVedtak = veilarbvedtaksstotteClient.hentSiste14AVedtak(fnr, accessToken)
        val personInfo = veilarbpersonClient.hentPersonInfo(fnr, accessToken)

        val brukerdata = Brukerdata(
            fnr = fnr,
            oppfolgingsenhet = oppfolgingsstatus?.oppfolgingsenhet,
            servicegruppe = oppfolgingsstatus?.servicegruppe,
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
    val servicegruppe: String?,
    val fornavn: String?,
    val manuellStatus: ManuellStatusDTO?
)
