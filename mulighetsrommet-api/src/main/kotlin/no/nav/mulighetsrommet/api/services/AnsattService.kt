package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.veileder.VeilarbveilederClient
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.TILTAKSANSVARLIG_FLATE_GRUPPE
import no.nav.poao_tilgang.client.AdGruppe
import java.util.*

class AnsattService(
    private val veilarbveilederClient: VeilarbveilederClient,
    private val poaoTilgangService: PoaoTilgangService,
    private val microsoftGraphService: MicrosoftGraphService
) {
    suspend fun hentAnsattData(accessToken: String, navAnsattAzureId: UUID): AnsattData {
        val data = veilarbveilederClient.hentVeilederdata(accessToken, navAnsattAzureId)
        val hovedenhet = microsoftGraphService.hentHovedEnhetForNavAnsatt(navAnsattAzureId)
        val azureAdGrupper = poaoTilgangService.hentAdGrupper(navAnsattAzureId)
        return AnsattData(
            etternavn = data?.etternavn,
            fornavn = data?.fornavn,
            ident = data?.ident,
            navn = data?.navn,
            tilganger = azureAdGrupper.mapNotNull(::mapAdGruppeTilTilgang).toSet(),
            hovedenhet = hovedenhet.hovedenhetKode,
            hovedenhetNavn = hovedenhet.hovedenhetNavn
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
data class AnsattData(
    val etternavn: String?,
    val fornavn: String?,
    val ident: String?,
    val navn: String?,
    val tilganger: Set<Tilgang>,
    val hovedenhet: String,
    val hovedenhetNavn: String
)

@Serializable
enum class Tilgang {
    FLATE
}
