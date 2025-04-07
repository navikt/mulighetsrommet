package no.nav.mulighetsrommet.api.navansatt

import no.nav.mulighetsrommet.api.AdGruppeNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MicrosoftGraphClient
import no.nav.mulighetsrommet.api.navansatt.api.NavAnsattFilter
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.*

class NavAnsattService(
    private val roles: Set<AdGruppeNavAnsattRolleMapping>,
    private val db: ApiDatabase,
    private val microsoftGraphClient: MicrosoftGraphClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSynchronizeNavAnsatt(azureId: UUID, accessType: AccessType): NavAnsatt = db.session {
        queries.ansatt.getByAzureId(azureId) ?: run {
            logger.info("Fant ikke NavAnsatt for azureId=$azureId i databasen, forsøker Azure AD i stedet")

            val ansatt = getNavAnsattFromAzure(azureId, accessType)
            queries.ansatt.upsert(NavAnsattDbo.fromNavAnsattDto(ansatt))

            val roles = getNavAnsattRoles(azureId, accessType)
            queries.ansatt.setRoller(ansatt.navIdent, roles)

            checkNotNull(queries.ansatt.getByAzureId(azureId))
        }
    }

    fun getNavAnsatte(filter: NavAnsattFilter): List<NavAnsatt> = db.session {
        val roller = filter.roller.map { Rolle.fromRolleAndEnheter(it, setOf()) }
        queries.ansatt.getAll(rollerContainsAll = roller)
    }

    suspend fun getNavAnsattFromAzureSok(query: String): List<AzureAdNavAnsatt> {
        return microsoftGraphClient.getNavAnsattSok(query)
    }

    suspend fun addUserToKontaktpersoner(navIdent: NavIdent): Unit = db.transaction {
        val kontaktPersonGruppeId = roles.find { it.rolle == NavAnsattRolle.KONTAKTPERSON }?.adGruppeId
        requireNotNull(kontaktPersonGruppeId)

        val ansatt = queries.ansatt.getByNavIdent(navIdent)
            ?: microsoftGraphClient.getNavAnsattByNavIdent(navIdent, AccessType.M2M)?.let {
                NavAnsatt.fromAzureAdNavAnsatt(it)
            }

        requireNotNull(ansatt) {
            "Fant ikke ansatt med navIdent=$navIdent i AzureAd"
        }

        if (ansatt.hasRole(Rolle.Kontaktperson)) {
            return
        }

        val roller = ansatt.roller + Rolle.Kontaktperson
        queries.ansatt.setRoller(ansatt.navIdent, roller)

        microsoftGraphClient.addToGroup(ansatt.azureId, kontaktPersonGruppeId)
    }

    suspend fun getNavAnsatteInGroups(groups: Set<AdGruppeNavAnsattRolleMapping>): List<NavAnsatt> {
        return groups
            .flatMap {
                val members = microsoftGraphClient.getGroupMembers(it.adGruppeId)
                logger.info("Fant ${members.size} i AD gruppe id=${it.adGruppeId}")
                members.map { NavAnsatt.fromAzureAdNavAnsatt(it) }
            }
            .toSet()
            .toList()
    }

    suspend fun getNavAnsattFromAzure(azureId: UUID, accessType: AccessType): NavAnsatt {
        val ansatt = microsoftGraphClient.getNavAnsatt(azureId, accessType)
        return NavAnsatt.fromAzureAdNavAnsatt(ansatt)
    }

    suspend fun getNavAnsattRoles(azureId: UUID, accessType: AccessType): Set<Rolle> {
        val rolesDirectory = roles.associateBy { it.adGruppeId }

        return microsoftGraphClient
            .getMemberGroups(azureId, accessType)
            .filter { rolesDirectory.containsKey(it.id) }
            .groupBy { rolesDirectory.getValue(it.id).rolle }
            .map { (rolle, groups) ->
                val enheter = groups.mapNotNull { resolveNavEnhetFromRolle(it.navn) }.toSet()
                Rolle.fromRolleAndEnheter(rolle, enheter)
            }
            .toSet()
    }

    private fun resolveNavEnhetFromRolle(navn: String): NavEnhetNummer? {
        val navEnhetRegex = "^(\\d{4})-.+$".toRegex()
        return navEnhetRegex.find(navn)?.groupValues?.get(1)?.let { NavEnhetNummer(it) }
    }
}
