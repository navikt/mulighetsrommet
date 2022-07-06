package no.nav.mulighetsrommet.api.clients.oppfolging

interface VeilarboppfolgingClient {
    suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String?) // TODO Må returnere data
}
