package no.nav.mulighetsrommet.api.domain.navansatt

object NavAnsattRolleHelper {
    fun hasRole(
        roles: Set<NavAnsattRolle>,
        requiredRole: NavAnsattRolle,
    ): Boolean = roles.any { role ->
        role.rolle == requiredRole.rolle && (role.generell || roleHasRequiredEnheter(role, requiredRole))
    }

    private fun roleHasRequiredEnheter(
        role: NavAnsattRolle,
        requiredRole: NavAnsattRolle,
    ): Boolean = !requiredRole.generell &&
        role.enheter.isNotEmpty() &&
        role.enheter.containsAll(requiredRole.enheter)
}
