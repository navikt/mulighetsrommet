package no.nav.mulighetsrommet.api.navansatt

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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

private val NAV_ENHET_GENERELL = NavEnhetNummer("0000")

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
            queries.ansatt.upsert(NavAnsattDbo.fromNavAnsatt(ansatt))
            queries.ansatt.setRoller(ansatt.navIdent, ansatt.roller)

            checkNotNull(queries.ansatt.getByAzureId(azureId))
        }
    }

    fun getNavAnsatte(filter: NavAnsattFilter): List<NavAnsatt> = db.session {
        val roller = filter.roller.map { NavAnsattRolle.generell(it) }
        queries.ansatt.getAll(rollerContainsAll = roller)
    }

    suspend fun getNavAnsattFromAzureSok(query: String): List<AzureAdNavAnsatt> {
        return microsoftGraphClient.getNavAnsattSok(query)
    }

    fun getNavAnsattByNavIdent(navIdent: NavIdent): NavAnsatt? = db.session {
        queries.ansatt.getByNavIdent(navIdent)
    }

    suspend fun addUserToKontaktpersoner(navIdent: NavIdent): Unit = db.transaction {
        val kontaktPersonGruppeId = roles.find { it.rolle == Rolle.KONTAKTPERSON }?.adGruppeId
        requireNotNull(kontaktPersonGruppeId)

        val ansatt = queries.ansatt.getByNavIdent(navIdent) ?: getNavAnsattFromAzure(navIdent, AccessType.M2M)
        if (ansatt.hasGenerellRolle(Rolle.KONTAKTPERSON)) {
            return
        }

        val roller = ansatt.roller + NavAnsattRolle.generell(Rolle.KONTAKTPERSON)
        queries.ansatt.setRoller(ansatt.navIdent, roller)

        microsoftGraphClient.addToGroup(ansatt.azureId, kontaktPersonGruppeId)
    }

    suspend fun getNavAnsatteInGroups(groups: Set<AdGruppeNavAnsattRolleMapping>): List<NavAnsatt> = coroutineScope {
        // For å begrense antall parallelle requests mot msgraph
        val semaphore = Semaphore(permits = 20)
        groups
            .map { group ->
                async {
                    semaphore.withPermit {
                        microsoftGraphClient.getGroupMembers(group.adGruppeId).also {
                            logger.info("Fant ${it.size} i AD gruppe id=${group.adGruppeId}")
                        }
                    }
                }
            }
            .awaitAll()
            .flatMapTo(mutableSetOf()) { it }
            .map { ansatt ->
                async { semaphore.withPermit { toNavAnsatt(ansatt, AccessType.M2M) } }
            }
            .awaitAll()
    }

    suspend fun getNavAnsattFromAzure(azureId: UUID, accessType: AccessType): NavAnsatt {
        val ansatt = microsoftGraphClient.getNavAnsatt(azureId, accessType)
        return toNavAnsatt(ansatt, accessType)
    }

    suspend fun getNavAnsattFromAzure(navIdent: NavIdent, accessType: AccessType): NavAnsatt {
        val ansatt = checkNotNull(microsoftGraphClient.getNavAnsattByNavIdent(navIdent, accessType))
        return toNavAnsatt(ansatt, accessType)
    }

    suspend fun getNavAnsattRoles(azureId: UUID, accessType: AccessType): Set<NavAnsattRolle> {
        val rolesDirectory = roles.groupBy { it.adGruppeId }

        val roleToEnheter = buildMap {
            microsoftGraphClient.getMemberGroups(azureId, accessType).forEach { group ->
                rolesDirectory[group.id]?.forEach { (_, rolle) ->
                    val enheter = resolveNavEnhetFromRolle(group.navn)
                    computeIfAbsent(rolle) { mutableSetOf() }.addAll(enheter)
                }
            }
        }

        /**
         * EntraId-grupper i Nav har ofte en navnestandard der de begynner med et enhetsnummer, eller 0000 når den er
         * generell (ikke enhetsspesifikk).
         *
         * Vi sier at rollen er generell om 0000 finnes blandt minst én av EntraId-gruppene som tildeler rollen, eller
         * om ingen av EntraId-gruppene som tildeler rollen definerer et enhetsnummer (og settet med Nav-enheter er
         * tomt).
         */
        return roleToEnheter.mapTo(mutableSetOf()) { (rolle, enheter) ->
            val generell = enheter.isEmpty() || NAV_ENHET_GENERELL in enheter
            NavAnsattRolle(rolle, generell, enheter.minus(NAV_ENHET_GENERELL))
        }
    }

    private suspend fun toNavAnsatt(ansatt: AzureAdNavAnsatt, accessType: AccessType): NavAnsatt {
        val roles = getNavAnsattRoles(ansatt.azureId, accessType)
        return ansatt.toNavAnsatt(roles)
    }

    private fun resolveNavEnhetFromRolle(navn: String): Set<NavEnhetNummer> {
        val navEnhetRegex = "^(\\d{4})-.+$".toRegex()

        return navEnhetRegex.find(navn)?.groupValues?.get(1)
            ?.let { NavEnhetNummer(it) }
            ?.let { enhetsnummer ->
                db.session {
                    queries.enhet.getAll(overordnetEnhet = enhetsnummer)
                        .mapTo(mutableSetOf()) { it.enhetsnummer }
                        .plus(enhetsnummer)
                }
            }
            ?: setOf()
    }
}

fun AzureAdNavAnsatt.toNavAnsatt(roles: Set<NavAnsattRolle>) = NavAnsatt(
    azureId = azureId,
    navIdent = navIdent,
    fornavn = fornavn,
    etternavn = etternavn,
    hovedenhet = NavAnsatt.Hovedenhet(
        enhetsnummer = hovedenhetKode,
        navn = hovedenhetNavn,
    ),
    mobilnummer = mobilnummer,
    epost = epost,
    roller = roles,
    skalSlettesDato = null,
)
