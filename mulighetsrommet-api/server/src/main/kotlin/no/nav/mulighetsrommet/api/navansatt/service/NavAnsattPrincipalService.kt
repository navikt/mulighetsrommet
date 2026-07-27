package no.nav.mulighetsrommet.api.navansatt.service

import com.auth0.jwt.interfaces.Payload
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPayloadHolder
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

typealias JwtId = String

class NavAnsattPrincipalService(
    private val navAnsattService: NavAnsattService,
    private val db: ApiDatabase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val roleCache: Cache<JwtId, Set<NavAnsattRolle>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    suspend fun resolveNavAnsattPrincipal(credentials: JWTCredential): NavAnsattMedRollerPrincipal? {
        val oid = credentials["oid"]?.let { UUID.fromString(it) } ?: run {
            log.warn("'oid' mangler i JWT credentials")
            return null
        }

        val navIdent = credentials["NAVident"]?.let { NavIdent(it) } ?: run {
            log.warn("'NAVident' mangler i JWT credentials")
            return null
        }

        val tokenId = credentials["uti"]?.takeIf { it.isNotEmpty() } ?: run {
            log.warn("'uti' mangler i JWT credentials")
            return null
        }

        val groups = credentials.getListClaim("groups", UUID::class)
        val roller = getRoles(tokenId, oid, groups)

        return NavAnsattMedRollerPrincipal(
            navAnsattObjectId = oid,
            navIdent = navIdent,
            roller = roller,
            payload = credentials.payload,
        )
    }

    private suspend fun getRoles(
        tokenId: JwtId,
        oid: UUID,
        groups: List<UUID>,
    ): Set<NavAnsattRolle> {
        roleCache.getIfPresent(tokenId)?.also { return it }

        return db.transaction {
            // Hvis rollene ikke finnes i cache så må de utledes fra appliksjonen i stedet.
            // For å unngå potensielle samtidighetsproblemer som kan oppstå om flere requests behandles
            // samtidig, enten i samme pod eller på tvers av pods, så skaffer vi oss en lås i forkant.
            // Hvis ansatt allerede er lagret i db så har ikke låsen en stor effekt, men den sørger
            // for at det kun er én request gjør et kall mot Entra samt lagrer ny ansatt db i de
            // tilfellenene der ansatt logger inn for aller første gang (og dermed ennå ikke finnes i db).
            aquireAdvisoryLock("nav-ansatt-sync:$oid")

            // Sjekk cachen på nytt i tilfelle en annen request rakk å populere den mens vi ventet på låsen
            roleCache.getIfPresent(tokenId)?.also { return@transaction it }

            syncNavAnsattRoller(oid, groups).also { roleCache.put(tokenId, it) }
        }
    }

    private suspend fun TransactionalQueryContext.syncNavAnsattRoller(
        oid: UUID,
        groups: List<UUID>,
    ): Set<NavAnsattRolle> {
        val roller = navAnsattService.getNavAnsattRolesFromGroups(groups)

        val ansatt = queries.ansatt.getByEntraObjectId(oid) ?: run {
            log.info("Fant ikke NavAnsatt for oid=$oid i databasen, henter fra Entra i stedet")
            val ansatt = navAnsattService.getNavAnsattFromAzure(oid, AccessType.M2M).medRoller(roller)
            queries.ansatt.save(ansatt)
            ansatt
        }

        if (ansatt.roller != roller) {
            log.info("Oppdaterer roller for ansatt med navIdent=${ansatt.navIdent} fra ${ansatt.roller} til $roller")
            queries.ansatt.save(ansatt.medRoller(roller))
        }

        return roller
    }
}

class NavAnsattMedRollerPrincipal(
    val navAnsattObjectId: UUID,
    val navIdent: NavIdent,
    val roller: Set<NavAnsattRolle>,
    payload: Payload,
) : JWTPayloadHolder(payload)
