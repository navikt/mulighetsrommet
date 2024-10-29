package no.nav.mulighetsrommet.api.services

import kotliquery.Session
import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.routes.v1.NavAnsattFilter
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.*

class NavAnsattService(
    private val roles: List<AdGruppeNavAnsattRolleMapping>,
    private val microsoftGraphClient: MicrosoftGraphClient,
    private val navAnsattRepository: NavAnsattRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSynchronizeNavAnsatt(azureId: UUID, accessType: AccessType): NavAnsattDto {
        return navAnsattRepository.getByAzureId(azureId) ?: run {
            logger.info("Fant ikke NavAnsatt for azureId=$azureId i databasen, forsøker Azure AD i stedet")
            val ansatt = getNavAnsattFromAzure(azureId, accessType)
            navAnsattRepository.upsert(NavAnsattDbo.fromNavAnsattDto(ansatt))
            ansatt
        }
    }

    fun getNavAnsatte(filter: NavAnsattFilter): List<NavAnsattDto> {
        return navAnsattRepository.getAll(roller = filter.roller)
    }

    suspend fun addUserToKontaktpersoner(navIdent: NavIdent, tx: Session) {
        val kontaktPersonGruppeId = roles.find { it.rolle == NavAnsattRolle.KONTAKTPERSON }?.adGruppeId
        requireNotNull(kontaktPersonGruppeId)

        val ansatt = navAnsattRepository.getByNavIdent(navIdent)
            ?: microsoftGraphClient.getNavAnsattByNavIdent(navIdent, AccessType.M2M)?.let {
                NavAnsattDto.fromAzureAdNavAnsatt(it, roller = emptySet())
            }
        requireNotNull(ansatt) {
            "Fant ikke ansatt med navIdent=$navIdent i AzureAd"
        }

        if (ansatt.roller.contains(NavAnsattRolle.KONTAKTPERSON)) {
            return
        }

        navAnsattRepository.upsert(
            NavAnsattDbo.fromNavAnsattDto(ansatt).copy(roller = ansatt.roller.plus(NavAnsattRolle.KONTAKTPERSON)),
            tx,
        )

        microsoftGraphClient.addToGroup(ansatt.azureId, kontaktPersonGruppeId)
    }

    suspend fun getNavAnsattFromAzureSok(query: String): List<NavAnsattDto> {
        return microsoftGraphClient.getNavAnsattSok(query)
            .map { NavAnsattDto.fromAzureAdNavAnsatt(it, emptySet()) }
    }

    suspend fun getNavAnsattFromAzure(azureId: UUID, accessType: AccessType): NavAnsattDto {
        val rolesDirectory = roles.associateBy { it.adGruppeId }

        val roller = microsoftGraphClient.getMemberGroups(azureId, accessType)
            .filter { rolesDirectory.containsKey(it.id) }
            .map { rolesDirectory.getValue(it.id).rolle }
            .toSet()

        if (roller.isEmpty()) {
            logger.info("Ansatt med azureId=$azureId har ingen av rollene $roles")
            throw IllegalStateException("Ansatt med azureId=$azureId har ingen av de påkrevde rollene")
        }

        val ansatt = microsoftGraphClient.getNavAnsatt(azureId, AccessType.M2M)
        return NavAnsattDto.fromAzureAdNavAnsatt(ansatt, roller)
    }

    suspend fun getNavAnsatteFromAzure(): List<NavAnsattDto> {
        return roles
            .flatMap {
                val members = microsoftGraphClient.getGroupMembers(it.adGruppeId)
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
