package no.nav.mulighetsrommet.api.domain.navansatt

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class NavAnsatt(
    @Serializable(with = UUIDSerializer::class)
    val entraObjectId: UUID,
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    val hovedenhet: NavEnhetNummer,
    val mobilnummer: String?,
    val epost: String,
    val roller: Set<NavAnsattRolle>,
    @Serializable(with = LocalDateSerializer::class)
    val skalSlettesDato: LocalDate?,
) {
    fun medRoller(roller: Set<NavAnsattRolle>): NavAnsatt = copy(roller = roller)

    fun skalSlettes(dato: LocalDate): NavAnsatt = copy(skalSlettesDato = dato, roller = setOf())

    fun hasGenerellRolle(
        rolle: Rolle,
    ): Boolean = NavAnsattRolleHelper.hasRole(roller, NavAnsattRolle.generell(rolle))

    fun hasKontorspesifikkRolle(
        rolle: Rolle,
        enheter: Set<NavEnhetNummer>,
    ): Boolean = NavAnsattRolleHelper.hasRole(roller, NavAnsattRolle.kontorspesifikk(rolle, enheter))

    fun hasAnyGenerellRolle(requiredRole: Rolle, vararg otherRoles: Rolle): Boolean {
        return setOf(requiredRole, *otherRoles).any { hasGenerellRolle(it) }
    }

    fun displayName(): String = "$fornavn $etternavn"
}
