package no.nav.mulighetsrommet.api.navansatt.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class NavAnsatt(
    @Serializable(with = UUIDSerializer::class)
    val azureId: UUID,
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    val hovedenhet: Hovedenhet,
    val mobilnummer: String?,
    val epost: String,
    val roller: Set<NavAnsattRolle>,
    @Serializable(with = LocalDateSerializer::class)
    val skalSlettesDato: LocalDate?,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )

    fun hasGenerellRolle(
        rolle: Rolle,
    ): Boolean = hasRolle(NavAnsattRolle.generell(rolle))

    fun hasKontorspesifikkRolle(
        rolle: Rolle,
        enheter: Set<NavEnhetNummer>,
    ): Boolean = hasRolle(NavAnsattRolle.kontorspesifikk(rolle, enheter))

    fun hasAnyGenerellRolle(requiredRole: Rolle, vararg otherRoles: Rolle): Boolean {
        return setOf(requiredRole, *otherRoles).any { hasGenerellRolle(it) }
    }

    private fun hasRolle(
        requiredRolle: NavAnsattRolle,
    ): Boolean = when (requiredRolle.generell) {
        true -> roller.any { it.rolle == requiredRolle.rolle }
        false -> roller.any {
            it.rolle == requiredRolle.rolle && (it.generell || it.enheter.containsAll(requiredRolle.enheter))
        }
    }
}
