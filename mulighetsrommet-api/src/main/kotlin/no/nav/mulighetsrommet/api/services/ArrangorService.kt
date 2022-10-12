package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.amtenhetsregister.AmtEnhetsregisterClient
import no.nav.mulighetsrommet.api.clients.arenaordsproxy.ArenaOrdsProxyClient

class ArrangorService(
    private val arenaOrdsProxyClient: ArenaOrdsProxyClient,
    private val amtEnhetsregisterClient: AmtEnhetsregisterClient
) {
    suspend fun hentArrangorNavn(arrangorId: Int): String? {
        val arbeidsgiverInfo = arenaOrdsProxyClient.hentArbeidsgiver(arrangorId)
        val virksomhetInfo =
            arbeidsgiverInfo?.virksomhetsnummer?.let { amtEnhetsregisterClient.hentVirksomhetsNavn(it.toInt()) }
        return virksomhetInfo?.navn
    }
}
