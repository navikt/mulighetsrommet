package no.nav.mulighetsrommet.api.clients.person

interface VeilarbpersonClient {
    suspend fun hentPersonInfo(fnr: String, accessToken: String): PersonDto
}
