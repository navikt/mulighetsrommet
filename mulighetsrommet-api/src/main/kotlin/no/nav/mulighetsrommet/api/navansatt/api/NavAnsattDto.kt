package no.nav.mulighetsrommet.api.navansatt.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.msgraph.EntraNavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class NavAnsattDto(
    @Serializable(with = UUIDSerializer::class)
    val entraObjectId: UUID,
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    val hovedenhet: Hovedenhet,
    val mobilnummer: String?,
    val epost: String,
    val roller: Set<Rolle>,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )

    companion object {
        fun fromNavAnsatt(ansatt: NavAnsatt): NavAnsattDto = NavAnsattDto(
            entraObjectId = ansatt.entraObjectId,
            navIdent = ansatt.navIdent,
            fornavn = ansatt.fornavn,
            etternavn = ansatt.etternavn,
            hovedenhet = Hovedenhet(
                enhetsnummer = ansatt.hovedenhet.enhetsnummer,
                navn = ansatt.hovedenhet.navn,
            ),
            mobilnummer = ansatt.mobilnummer,
            epost = ansatt.epost,
            roller = ansatt.roller.map { it.rolle }.toSet(),
        )

        fun fromEntraNavAnsatt(dto: EntraNavAnsatt): NavAnsattDto = NavAnsattDto(
            entraObjectId = dto.entraObjectId,
            navIdent = dto.navIdent,
            fornavn = dto.fornavn,
            etternavn = dto.etternavn,
            hovedenhet = Hovedenhet(
                enhetsnummer = dto.hovedenhetKode,
                navn = dto.hovedenhetNavn,
            ),
            mobilnummer = dto.mobilnummer,
            epost = dto.epost,
            roller = setOf(),
        )
    }
}
