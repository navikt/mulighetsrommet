package no.nav.mulighetsrommet.api.clients.arena_ords_proxy

import no.nav.mulighetsrommet.api.domain.ArrangorDTO

interface ArenaOrdsProxyClient {
    suspend fun hentArbeidsgiver(arbeidsgiverId: Int): ArrangorDTO?
}
