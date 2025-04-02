package no.nav.mulighetsrommet.api.navansatt.db

import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.util.*

data class NavAnsattDbo(
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    /**
     * Enhetsnummer til den ansattes hovedenhet.
     */
    val hovedenhet: String,
    val azureId: UUID,
    val mobilnummer: String?,
    val epost: String,
    val skalSlettesDato: LocalDate?,
) {
    companion object {
        fun fromNavAnsattDto(dto: NavAnsattDto): NavAnsattDbo = NavAnsattDbo(
            navIdent = dto.navIdent,
            fornavn = dto.fornavn,
            etternavn = dto.etternavn,
            hovedenhet = dto.hovedenhet.enhetsnummer,
            azureId = dto.azureId,
            mobilnummer = dto.mobilnummer,
            epost = dto.epost,
            skalSlettesDato = dto.skalSlettesDato,
        )
    }
}
