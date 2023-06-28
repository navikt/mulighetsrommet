package no.nav.mulighetsrommet.api.services

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.utils.NavAnsattFilter
import no.nav.mulighetsrommet.database.utils.getOrThrow
import org.slf4j.LoggerFactory
import java.util.*

class NavAnsattService(
    private val microsoftGraphService: MicrosoftGraphService,
    private val ansatte: NavAnsattRepository,
    private val roles: List<AdGruppeNavAnsattRolleMapping> = emptyList(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getNavAnsatt(azureId: UUID): NavAnsattDto {
        return ansatte.getByAzureId(azureId)
            .map {
                if (it != null) {
                    it
                } else {
                    logger.info("Fant ikke NavAnsatt for azureId=$azureId i databasen, forsøker Azure AD i stedet")
                    getNavAnsattFromAzure("", azureId)
                }
            }
            .getOrThrow()
    }

    fun getNavAnsatte(filter: NavAnsattFilter): List<NavAnsattDto> {
        return ansatte.getAll(roller = filter.roller).getOrThrow()
    }

    suspend fun getNavAnsatteWithRoles(roles: List<AdGruppeNavAnsattRolleMapping>): List<NavAnsattDto> {
        return roles
            .flatMap {
                val members = microsoftGraphService.getNavAnsatteInGroup(it.adGruppeId)
                members.map { ansatt ->
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt, listOf(it.rolle))
                }
            }
            .groupBy { it.navIdent }
            .map { (_, value) ->
                value.reduce { a1, a2 ->
                    a1.copy(roller = a1.roller + a2.roller)
                }
            }
    }

    suspend fun getNavAnsattFromAzure(accessToken: String, azureId: UUID): NavAnsattDto = coroutineScope {
        val ansatt = async { microsoftGraphService.getNavAnsatt(accessToken, azureId) }
        val groups = async { microsoftGraphService.getNavAnsattAdGrupper(accessToken, azureId) }

        val rolesDirectory = roles.associateBy { it.adGruppeId }

        val roller = groups
            .await()
            .filter { rolesDirectory.containsKey(it.id) }
            .map { rolesDirectory.getValue(it.id).rolle }

        NavAnsattDto.fromAzureAdNavAnsatt(ansatt.await(), roller)
    }

    suspend fun getNavAnsatteFromAzure(): List<NavAnsattDto> {
        return roles
            .flatMap {
                val members = microsoftGraphService.getNavAnsatteInGroup(it.adGruppeId)
                members.map { ansatt ->
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt, listOf(it.rolle))
                }
            }
            .groupBy { it.navIdent }
            .map { (_, value) ->
                value.reduce { a1, a2 ->
                    a1.copy(roller = a1.roller + a2.roller)
                }
            }
    }
}

data class AdGruppeNavAnsattRolleMapping(
    val adGruppeId: UUID,
    val rolle: NavAnsattRolle,
)
