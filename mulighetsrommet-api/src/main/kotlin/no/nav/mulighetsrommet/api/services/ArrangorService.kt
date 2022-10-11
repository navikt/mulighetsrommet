package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.arenaordsproxy.ArenaOrdsProxyClient

class ArrangorService(
    private val arenaOrdsProxyClient: ArenaOrdsProxyClient
) {
    fun hentArrangorNavn(arrangorId: Int): String {
        return ""
    }
}
