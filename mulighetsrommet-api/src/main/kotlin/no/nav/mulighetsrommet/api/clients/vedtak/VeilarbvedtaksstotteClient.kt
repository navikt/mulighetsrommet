package no.nav.mulighetsrommet.api.clients.vedtak

import no.nav.mulighetsrommet.api.domain.VedtakDTO

interface VeilarbvedtaksstotteClient {
    suspend fun hentSiste14AVedtak(fnr: String, accessToken: String?) : VedtakDTO?
}
