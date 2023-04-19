package no.nav.mulighetsrommet.api.clients.norg2

interface Norg2Client {
    suspend fun hentEnheter(): List<Norg2Response>
}
