package no.nav.mulighetsrommet.api.navansatt.service

import com.auth0.jwt.interfaces.Payload
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.auth.jwt.*
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.model.NavIdent
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

typealias JwtSessionId = String

class NavAnsattPrincipalService(
    private val navAnsattService: NavAnsattService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val roleCache: Cache<JwtSessionId, Set<NavAnsattRolle>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .recordStats()
        .build()

    fun resolveNavAnsattPrincipal(credentials: JWTCredential): NavAnsattPrincipal? {
        val navAnsattAzureId = credentials["oid"]?.let { UUID.fromString(it) } ?: run {
            log.warn("'oid' mangler i JWT credentials")
            return null
        }

        val navIdent = credentials["NAVident"]?.let { NavIdent(it) } ?: run {
            log.warn("'NAVident' mangler i JWT credentials")
            return null
        }

        val sessionId = credentials["sid"] ?: run {
            log.warn("'sid' mangler i JWT credentials")
            return null
        }

        val groups = credentials.getListClaim("groups", UUID::class)
        val roller = getRoles(sessionId, groups)

        return NavAnsattPrincipal(
            navAnsattOid = navAnsattAzureId,
            navIdent = navIdent,
            roller = roller,
            payload = credentials.payload,
        )
    }

    fun getRoles(sessionId: JwtSessionId, groups: List<UUID>): Set<NavAnsattRolle> {
        return roleCache.getIfPresent(sessionId) ?: navAnsattService.getNavAnsattRolesFromGroups(groups).also {
            roleCache.put(sessionId, it)
        }
    }
}

class NavAnsattPrincipal(
    val navAnsattOid: UUID,
    val navIdent: NavIdent,
    val roller: Set<NavAnsattRolle>,
    payload: Payload,
) : JWTPayloadHolder(payload)
