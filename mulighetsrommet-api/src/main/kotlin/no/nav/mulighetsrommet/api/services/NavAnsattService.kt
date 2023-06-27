package no.nav.mulighetsrommet.api.services

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
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
    private val ansatte: NavAnsattRepository,
) {
    suspend fun hentAnsattData(accessToken: String, navAnsattAzureId: UUID): AnsattData {
        val ansatt = microsoftGraphService.getNavAnsatt(accessToken, navAnsattAzureId)
        val azureAdGrupper = microsoftGraphService.getNavAnsattAdGrupper(accessToken, navAnsattAzureId)
        return AnsattData(
            etternavn = ansatt.etternavn,
            fornavn = ansatt.fornavn,
            ident = ansatt.navIdent,
            navn = "${ansatt.fornavn} ${ansatt.etternavn}",
            tilganger = azureAdGrupper.mapNotNull(::mapAdGruppeTilTilgang).toSet(),
            hovedenhet = ansatt.hovedenhetKode,
            hovedenhetNavn = ansatt.hovedenhetNavn,
        )
    }

    fun hentKontaktpersoner(filter: NavAnsattFilter): List<NavKontaktpersonDto> {
        return ansatte.getAll(roller = filter.roller)
            .map { ansatte ->
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

    suspend fun getNavAnsatteWithRoles(roles: List<AdGruppeNavAnsattRolleMapping>) = roles
        .flatMap {
            val members = microsoftGraphService.getNavAnsatteInGroup(it.adGruppeId)
            members.map { ansatt ->
                NavAnsattDbo.fromDto(ansatt, listOf(it.rolle))
            }
        }
        .groupBy { it.navIdent }
        .map { (_, value) ->
            value.reduce { a1, a2 ->
                a1.copy(roller = a1.roller + a2.roller)
            }
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

data class AdGruppeNavAnsattRolleMapping(
    val adGruppeId: UUID,
    val rolle: NavAnsattRolle,
)
