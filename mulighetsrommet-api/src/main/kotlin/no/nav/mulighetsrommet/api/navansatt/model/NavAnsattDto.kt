package no.nav.mulighetsrommet.api.navansatt.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class NavAnsattDto(
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
                is Rolle.Kontorspesifikk -> it.rolle == requiredRole.rolle && it.enheter.containsAll(requiredRole.enheter)
                else -> false
            }
        }
    }

    companion object {
        // TODO: office specific roles
        fun fromAzureAdNavAnsatt(dto: AzureAdNavAnsatt, roller: Set<NavAnsattRolle>): NavAnsattDto = NavAnsattDto(
            azureId = dto.azureId,
            navIdent = dto.navIdent,
            fornavn = dto.fornavn,
            etternavn = dto.etternavn,
            hovedenhet = Hovedenhet(
                enhetsnummer = dto.hovedenhetKode,
                navn = dto.hovedenhetNavn,
            ),
            mobilnummer = dto.mobilnummer,
            epost = dto.epost,
            roller = roller.map { Rolle.fromRolleAndEnheter(it) }.toSet(),
            skalSlettesDato = null,
        )
    }
}
