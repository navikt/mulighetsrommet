package no.nav.mulighetsrommet.api.clients.dialog

import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogResponse

interface VeilarbdialogClient {
    suspend fun sendMeldingTilDialogen(fnr: String, requestBody: DialogRequest): DialogResponse?
}
