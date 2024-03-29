package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient

class DialogService(
    private val veilarbdialogClient: VeilarbdialogClient,
) {
    suspend fun sendMeldingTilDialogen(fnr: String, accessToken: String, dialogRequest: DialogRequest): DialogResponse? {
        return veilarbdialogClient.sendMeldingTilDialogen(fnr, accessToken, dialogRequest)
    }
}

@Serializable
data class DialogRequest(
    val norskIdent: String,
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraBruker: Boolean,
)

@Serializable
data class DialogResponse(
    val id: String,
)
