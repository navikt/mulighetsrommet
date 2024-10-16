package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.utils.NavAnsattFilter
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.*

class NavAnsattService(
    private val roles: List<AdGruppeNavAnsattRolleMapping>,
    private val microsoftGraphService: MicrosoftGraphService,
    private val navAnsattRepository: NavAnsattRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSynchronizeNavAnsatt(azureId: UUID): NavAnsattDto {
        return navAnsattRepository.getByAzureId(azureId) ?: run {
            logger.info("Fant ikke NavAnsatt for azureId=$azureId i databasen, forsøker Azure AD i stedet")
            val ansatt = getNavAnsattFromAzure(azureId)
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansatt))
            ansatt
        }
    }

    fun getNavAnsatte(filter: NavAnsattFilter): List<NavAnsattDto> {
        return navAnsattRepository.getAll(roller = filter.roller)
    }

    suspend fun getNavAnsattFromAzure(azureId: UUID): NavAnsattDto {
        val rolesDirectory = roles.associateBy { it.adGruppeId }

        val roller = microsoftGraphService.getNavAnsattAdGrupper(azureId, AccessType.M2M)
            .filter { rolesDirectory.containsKey(it.id) }
            .map { rolesDirectory.getValue(it.id).rolle }
            .toSet()

        if (roller.isEmpty()) {
            logger.info("Ansatt med azureId=$azureId har ingen av rollene $roles")
            throw IllegalStateException("Ansatt med azureId=$azureId har ingen av de påkrevde rollene")
        }

        val ansatt = microsoftGraphService.getNavAnsatt(azureId, AccessType.M2M)
        return NavAnsattDto.fromAzureAdNavAnsatt(ansatt, roller)
    }

    suspend fun getNavAnsatteFromAzure(): List<NavAnsattDto> {
        return roles
            .flatMap {
                val members = microsoftGraphService.getNavAnsatteInGroup(it.adGruppeId)
                logger.info("Fant ${members.size} i AD gruppe id=${it.adGruppeId}")
                members.map { ansatt ->
                    NavAnsattDto.fromAzureAdNavAnsatt(ansatt, setOf(it.rolle))
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
