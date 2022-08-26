package no.nav.mulighetsrommet.api.clients.person

import no.nav.mulighetsrommet.api.domain.PersonDTO

interface VeilarbpersonClient {
    suspend fun hentPersonInfo(fnr: String, accessToken: String?): PersonDTO?
}
