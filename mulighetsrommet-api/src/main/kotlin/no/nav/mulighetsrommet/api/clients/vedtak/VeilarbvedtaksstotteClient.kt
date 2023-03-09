package no.nav.mulighetsrommet.api.clients.vedtak

interface VeilarbvedtaksstotteClient {
    suspend fun hentSiste14AVedtak(fnr: String, accessToken: String): VedtakDto?
}
