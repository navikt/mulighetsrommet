package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.AdGruppe
import no.nav.mulighetsrommet.api.domain.dto.NavKontaktpersonDto
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.ADMIN_FLATE_BETABRUKER
import no.nav.mulighetsrommet.api.tilgangskontroll.AdGrupper.TEAM_MULIGHETSROMMET
import no.nav.mulighetsrommet.api.utils.NavAnsattFilter
import no.nav.mulighetsrommet.database.utils.getOrThrow
import java.util.*

class NavAnsattService(
    private val microsoftGraphService: MicrosoftGraphService,
    private val navAnsattRepository: NavAnsattRepository,
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

    fun hentAnsatte(filter: NavAnsattFilter): List<NavKontaktpersonDto> {
        return navAnsattRepository.getAll(filter).map { ansatte ->
            ansatte.map {
                NavKontaktpersonDto(
                    navident = it.navIdent,
                    azureId = it.azureId,
                    fornavn = it.fornavn,
                    etternavn = it.etternavn,
                    hovedenhetKode = it.hovedenhet,
                    mobilnr = it.mobilnummer,
                    epost = it.epost,
                )
            }
        }.getOrThrow()
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
