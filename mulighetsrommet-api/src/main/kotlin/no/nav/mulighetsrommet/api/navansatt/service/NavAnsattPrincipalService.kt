package no.nav.mulighetsrommet.api.navansatt.service

import com.auth0.jwt.interfaces.Payload
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.auth.jwt.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.*
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

    suspend fun resolveNavAnsattPrincipal(credentials: JWTCredential): NavAnsattPrincipal? {
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

        return NavAnsattPrincipal(
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

        val roller = navAnsattService.getNavAnsattRolesFromGroups(groups)
        syncNavAnsattRoller(oid, roller)

        roleCache.put(tokenId, roller)

        return roller
    }

    private suspend fun syncNavAnsattRoller(oid: UUID, roller: Set<NavAnsattRolle>): Unit = db.session {
        val ansatt = queries.ansatt.getByAzureId(oid) ?: run {
            log.info("Fant ikke NavAnsatt for azureId=$oid i databasen, henter fra Entra i stedet")
            val ansatt = navAnsattService.getNavAnsattFromAzure(oid, AccessType.M2M)
            queries.ansatt.upsert(NavAnsattDbo.fromNavAnsatt(ansatt))
            ansatt
        }

        if (ansatt.roller != roller) {
            log.info("Oppdaterer roller for ansatt med navIdent=${ansatt.navIdent} fra ${ansatt.roller} til $roller")
            queries.ansatt.setRoller(ansatt.navIdent, roller)
        }
    }
}

class NavAnsattPrincipal(
    val navAnsattObjectId: UUID,
    val navIdent: NavIdent,
    val roller: Set<NavAnsattRolle>,
    payload: Payload,
) : JWTPayloadHolder(payload)
