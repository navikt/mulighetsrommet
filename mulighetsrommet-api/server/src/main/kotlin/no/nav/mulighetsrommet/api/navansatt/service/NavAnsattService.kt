package no.nav.mulighetsrommet.api.navansatt.service

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.EntraGroupNavAnsattRolleMapping
import no.nav.mulighetsrommet.api.clients.msgraph.EntraNavAnsatt
import no.nav.mulighetsrommet.api.clients.msgraph.MsGraphClient
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.UUID

class NavAnsattService(
    private val roles: Set<EntraGroupNavAnsattRolleMapping>,
    private val db: ApiDatabase,
    private val microsoftGraphClient: MsGraphClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getOrSynchronizeNavAnsatt(navIdent: NavIdent, accessType: AccessType): NavAnsatt = db.transaction {
        queries.ansatt.get(navIdent) ?: run {
            logger.info("Fant ikke NavAnsatt for navIdent=$navIdent i databasen, forsøker Azure AD i stedet")

            val ansatt = getNavAnsattFromAzure(navIdent, accessType)
            queries.ansatt.save(ansatt)
            queries.ansatt.getOrError(navIdent)
        }
    }

    suspend fun getNavAnsattFromAzureSok(query: String): List<EntraNavAnsatt> {
        return microsoftGraphClient.getNavAnsattSok(query)
    }

    fun getNavAnsattByNavIdent(navIdent: NavIdent): NavAnsatt? = db.session {
        queries.ansatt.get(navIdent)
    }

    suspend fun addUserToKontaktpersoner(navIdent: NavIdent): Unit = db.transaction {
        val kontaktPersonGruppeId = roles.find { it.rolle == Rolle.KONTAKTPERSON }?.entraGroupId
        requireNotNull(kontaktPersonGruppeId)

        val ansatt = getOrSynchronizeNavAnsatt(navIdent, AccessType.M2M)
        if (ansatt.hasGenerellRolle(Rolle.KONTAKTPERSON)) {
            return
        }

        val roller = ansatt.roller + NavAnsattRolle.generell(Rolle.KONTAKTPERSON)
        queries.ansatt.save(ansatt.medRoller(roller))

        microsoftGraphClient.addToGroup(ansatt.entraObjectId, kontaktPersonGruppeId)
    }

    suspend fun getNavAnsatteForAllRoles(): List<NavAnsatt> = getNavAnsatteForRoles(roles)

    suspend fun getNavAnsatteForRoles(roles: Set<EntraGroupNavAnsattRolleMapping>): List<NavAnsatt> = coroutineScope {
        // For å begrense antall parallelle requests mot msgraph
        val semaphore = Semaphore(permits = 20)

        // Hent medlemmer for hver gruppe parallelt, og behold info om hvilken gruppe de tilhører
        val groupMembersWithRoles = roles
            .map { role ->
                async {
                    semaphore.withPermit {
                        val members = microsoftGraphClient.getGroupMembers(role.entraGroupId)
                        logger.info("Fant ${members.size} i AD gruppe id=${role.entraGroupId}")
                        role to members
                    }
                }
            }
            .awaitAll()

        val memberToRoles = mutableMapOf<UUID, MutableSet<EntraGroupNavAnsattRolleMapping>>()
        groupMembersWithRoles.forEach { (role, members) ->
            members.forEach { member ->
                memberToRoles.getOrPut(member.entraObjectId) { mutableSetOf() }.add(role)
            }
        }

        val uniqueMembers = groupMembersWithRoles.flatMapTo(mutableSetOf()) { it.second }
        uniqueMembers.map { ansatt ->
            val userRoles = memberToRoles[ansatt.entraObjectId] ?: emptySet()
            val ansattRoles = userRoles.map { it.toNavAnsattRolle() }.toSet()
            ansatt.toNavAnsatt(ansattRoles)
        }
    }

    suspend fun getNavAnsattFromAzure(oid: UUID, accessType: AccessType): NavAnsatt {
        val ansatt = microsoftGraphClient.getNavAnsatt(oid, accessType)
        return toNavAnsatt(ansatt, accessType)
    }

    suspend fun getNavAnsattFromAzure(navIdent: NavIdent, accessType: AccessType): NavAnsatt {
        val ansatt = checkNotNull(microsoftGraphClient.getNavAnsattByNavIdent(navIdent, accessType))
        return toNavAnsatt(ansatt, accessType)
    }

    suspend fun getNavAnsattNavnFromAzure(navIdent: NavIdent, accessType: AccessType): String {
        val ansatt = checkNotNull(microsoftGraphClient.getNavAnsattByNavIdent(navIdent, accessType))
        return "${ansatt.etternavn}, ${ansatt.fornavn}"
    }

    suspend fun getNavAnsattRoles(oid: UUID, accessType: AccessType): Set<NavAnsattRolle> {
        val groups = microsoftGraphClient.getMemberGroups(oid, accessType)
        return getNavAnsattRolesFromGroups(groups)
    }

    fun EntraGroupNavAnsattRolleMapping.toNavAnsattRolle(): NavAnsattRolle {
        val generell = kostnadssteder.isEmpty()
        val enheter = kostnadssteder.flatMapTo(mutableSetOf()) { withKostnadssteder(it) }
        return NavAnsattRolle(rolle, generell, enheter)
    }

    fun getNavAnsattRolesFromGroups(groups: List<UUID>): Set<NavAnsattRolle> {
        val rolesDirectory = roles.groupBy { it.entraGroupId }

        return groups
            .flatMap { rolesDirectory[it] ?: emptyList() }
            .groupBy { it.rolle }
            .map { (rolle, mappings) ->
                val generell = mappings.any { it.kostnadssteder.isEmpty() }
                val enheter = mappings.flatMapTo(mutableSetOf()) { mapping ->
                    mapping.kostnadssteder.flatMap { withKostnadssteder(it) }
                }
                NavAnsattRolle(rolle, generell, enheter)
            }.toSet()
    }

    private suspend fun toNavAnsatt(ansatt: EntraNavAnsatt, accessType: AccessType): NavAnsatt {
        val roles = getNavAnsattRoles(ansatt.entraObjectId, accessType)
        return ansatt.toNavAnsatt(roles)
    }

    private fun withKostnadssteder(enhetsnummer: NavEnhetNummer): Set<NavEnhetNummer> = db.session {
        queries.kostnadssted.getAll(regioner = listOf(enhetsnummer))
            .mapTo(mutableSetOf()) { it.enhetsnummer }
            .plus(enhetsnummer)
    }
}

fun EntraNavAnsatt.toNavAnsatt(roles: Set<NavAnsattRolle>) = NavAnsatt(
    entraObjectId = entraObjectId,
    navIdent = navIdent,
    fornavn = fornavn,
    etternavn = etternavn,
    hovedenhet = hovedenhetKode,
    mobilnummer = mobilnummer,
    epost = epost,
    roller = roles,
    skalSlettesDato = null,
)
