package no.nav.mulighetsrommet.api.clients.oppfolging

interface VeilarboppfolgingClient {
    suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String): OppfolgingsstatusDto?
    suspend fun hentManuellStatus(fnr: String, accessToken: String): ManuellStatusDto?
}
