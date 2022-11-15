package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClient

class VeilederService(
    private val veilarbveilederClient: VeilarbveilederClient
) {

    suspend fun hentVeilederdata(accessToken: String): VeilederData {
        val data = veilarbveilederClient.hentVeilederdata(accessToken)
        return VeilederData(
            etternavn = data?.etternavn,
            fornavn = data?.fornavn,
            ident = data?.ident,
            navn = data?.navn
        )
    }
}

@Serializable
data class VeilederData(
    val etternavn: String?,
    val fornavn: String?,
    val ident: String?,
    val navn: String?
)
