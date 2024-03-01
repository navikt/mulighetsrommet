package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.msgraph.AzureAdNavAnsatt
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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
    val roller: Set<NavAnsattRolle>,
    @Serializable(with = LocalDateSerializer::class)
    val skalSlettesDato: LocalDate?,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: String,
        val navn: String,
    )

    companion object {
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
            roller = roller,
            skalSlettesDato = null,
        )
    }
}
