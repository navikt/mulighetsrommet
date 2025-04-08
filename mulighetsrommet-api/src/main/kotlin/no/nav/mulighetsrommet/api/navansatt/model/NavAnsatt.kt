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
    val roller: Set<Rolle>,
    @Serializable(with = LocalDateSerializer::class)
    val skalSlettesDato: LocalDate?,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )

    fun hasRole(
        requiredRole: Rolle,
    ): Boolean = when (requiredRole) {
        is Rolle.Generell -> roller.any { it.rolle == requiredRole.rolle }

        is Rolle.Kontorspesifikk -> roller.any {
            when (it) {
                is Rolle.Kontorspesifikk ->
                    it.rolle == requiredRole.rolle &&
                        // TODO: fjern isEmpty()-sjekk når nye ad-grupper er på plass
                        (it.enheter.isEmpty() || it.enheter.containsAll(requiredRole.enheter))

                else -> false
            }
        }
    }
}
