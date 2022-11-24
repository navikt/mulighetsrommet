package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClient
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.TILTAKSANSVARLIG_FLATE_GRUPPE
import no.nav.poao_tilgang.client.AdGruppe
import java.util.*

class AnsattService(
    private val veilarbveilederClient: VeilarbveilederClient,
    private val poaoTilgangService: PoaoTilgangService
) {
    suspend fun hentAnsattData(accessToken: String, navAnsattAzureId: UUID): VeilederData {
        val data = veilarbveilederClient.hentVeilederdata(accessToken)
        val azureAdGrupper = poaoTilgangService.hentAdGrupper(navAnsattAzureId)
        print(azureAdGrupper)
        return VeilederData(
            etternavn = data?.etternavn,
            fornavn = data?.fornavn,
            ident = data?.ident,
            navn = data?.navn,
            tilganger = azureAdGrupper.mapNotNull(::mapAdGruppeTilTilgang).toSet()
        )
    }
}

private fun mapAdGruppeTilTilgang(adGruppe: AdGruppe): Tilgang? {
    return when (adGruppe.navn) {
        TILTAKSANSVARLIG_FLATE_GRUPPE -> Tilgang.FLATE
        else -> null
    }
}

@Serializable
data class VeilederData(
    val etternavn: String?,
    val fornavn: String?,
    val ident: String?,
    val navn: String?,
    val tilganger: Set<Tilgang>
)

@Serializable
enum class Tilgang {
    FLATE
}
