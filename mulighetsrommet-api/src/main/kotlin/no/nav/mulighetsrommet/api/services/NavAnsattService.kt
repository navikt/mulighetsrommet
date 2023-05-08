package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.ADMIN_FLATE_BETABRUKER
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.TEAM_MULIGHETSROMMET
import java.util.*

class NavAnsattService(
    private val microsoftGraphService: MicrosoftGraphService,
) {
    suspend fun hentAnsattData(accessToken: String, navAnsattAzureId: UUID): AnsattData {
        val ansatt = microsoftGraphService.getNavAnsatt(accessToken, navAnsattAzureId)
        val azureAdGrupper = microsoftGraphService.getNavAnsattAdGrupper(accessToken, navAnsattAzureId)
        return AnsattData(
            etternavn = ansatt.etternavn,
            fornavn = ansatt.fornavn,
            ident = ansatt.navident,
            navn = "${ansatt.fornavn} ${ansatt.etternavn}",
            tilganger = azureAdGrupper.mapNotNull(::mapAdGruppeTilTilgang).toSet(),
            hovedenhet = ansatt.hovedenhetKode,
            hovedenhetNavn = ansatt.hovedenhetNavn,
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
    val hovedenhetNavn: String,
)

@Serializable
enum class Tilgang {
    BETABRUKER,
    UTVIKLER_VALP,
}
