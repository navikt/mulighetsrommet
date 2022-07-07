package no.nav.mulighetsrommet.api.clients.oppfolging

import no.nav.mulighetsrommet.api.domain.Oppfolgingsstatus

interface VeilarboppfolgingClient {
    suspend fun hentOppfolgingsstatus(fnr: String, accessToken: String?): Oppfolgingsstatus?
}
