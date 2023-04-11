package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.ADMIN_FLATE_BETABRUKER
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.TEAM_MULIGHETSROMMET
import no.nav.poao_tilgang.client.AdGruppe
import java.util.*

class AnsattService(
    private val poaoTilgangService: PoaoTilgangService,
    private val microsoftGraphService: MicrosoftGraphService
) {
    suspend fun hentAnsattData(accessToken: String, navAnsattAzureId: UUID): AnsattData {
        val data = microsoftGraphService.hentAnsattData(accessToken, navAnsattAzureId)
        val azureAdGrupper = poaoTilgangService.hentAdGrupper(navAnsattAzureId)
        return AnsattData(
            etternavn = data.etternavn,
            fornavn = data.fornavn,
            ident = data.navident,
            navn = "${data.fornavn} ${data.etternavn}",
            tilganger = azureAdGrupper.mapNotNull(::mapAdGruppeTilTilgang).toSet(),
            hovedenhet = data.hovedenhetKode,
            hovedenhetNavn = data.hovedenhetNavn
        )
    }
}

private fun mapAdGruppeTilTilgang(adGruppe: AdGruppe): Tilgang? {
    return when (adGruppe.navn) {
        ADMIN_FLATE_BETABRUKER -> Tilgang.BETABRUKER
        TEAM_MULIGHETSROMMET -> Tilgang.UTVIKLER_VALP
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
    BETABRUKER,
    UTVIKLER_VALP
}
