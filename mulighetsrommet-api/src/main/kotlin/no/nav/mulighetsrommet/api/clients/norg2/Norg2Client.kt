package no.nav.mulighetsrommet.api.clients.norg2

import no.nav.mulighetsrommet.api.domain.Norg2Enhet

interface Norg2Client {
    suspend fun hentEnheter(): List<Norg2Enhet>
}
