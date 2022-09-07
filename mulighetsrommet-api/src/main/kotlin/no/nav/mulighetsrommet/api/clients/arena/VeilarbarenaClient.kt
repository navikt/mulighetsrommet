package no.nav.mulighetsrommet.api.clients.arena

interface VeilarbarenaClient {
    suspend fun hentPersonIdForFnr(fnr: String): String?
}
