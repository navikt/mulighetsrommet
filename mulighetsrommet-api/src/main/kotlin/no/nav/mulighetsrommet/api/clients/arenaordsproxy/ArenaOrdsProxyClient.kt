package no.nav.mulighetsrommet.api.clients.arenaordsproxy

import no.nav.mulighetsrommet.api.domain.ArbeidsgiverDTO

interface ArenaOrdsProxyClient {
    suspend fun hentArbeidsgiver(arbeidsgiverId: Int): ArbeidsgiverDTO?
}
