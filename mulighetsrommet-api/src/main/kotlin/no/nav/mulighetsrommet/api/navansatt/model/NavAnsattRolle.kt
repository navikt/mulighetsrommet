package no.nav.mulighetsrommet.api.navansatt.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer

@Serializable
data class NavAnsattRolle(
    val rolle: Rolle,
    val generell: Boolean,
    val enheter: Set<NavEnhetNummer>,
) {
    companion object {
        fun generell(rolle: Rolle): NavAnsattRolle {
            return NavAnsattRolle(
                rolle = rolle,
                generell = true,
                enheter = setOf(),
            )
        }

        fun kontorspesifikk(rolle: Rolle, enheter: Set<NavEnhetNummer>): NavAnsattRolle {
            return NavAnsattRolle(
                rolle = rolle,
                generell = false,
                enheter = enheter,
            )
        }
    }
}

object NavAnsattRolleHelper {
    fun hasRole(
        roles: Set<NavAnsattRolle>,
        requiredRole: NavAnsattRolle,
    ): Boolean = when (requiredRole.generell) {
        true -> roles.any { it.rolle == requiredRole.rolle }
        false -> roles.any {
            it.rolle == requiredRole.rolle && (it.generell || it.enheter.containsAll(requiredRole.enheter))
        }
    }
}
