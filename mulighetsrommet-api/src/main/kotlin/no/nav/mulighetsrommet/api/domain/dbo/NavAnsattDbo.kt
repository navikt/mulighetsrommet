package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import java.util.*

data class NavAnsattDbo(
    val navIdent: String,
    val fornavn: String,
    val etternavn: String,
    /**
     * Enhetsnummer til den ansattes hovedenhet.
     */
    val hovedenhet: String,
    val azureId: UUID,
    val mobilnummer: String? = null,
    val epost: String,
    val roller: List<NavAnsattRolle>,
) {
    companion object {
        fun fromDto(dto: NavAnsattDto, roller: List<NavAnsattRolle> = listOf()) = NavAnsattDbo(
            navIdent = dto.navident,
            fornavn = dto.fornavn,
            etternavn = dto.etternavn,
            hovedenhet = dto.hovedenhetKode,
            azureId = dto.azureId,
            mobilnummer = dto.mobilnr,
            epost = dto.epost,
            roller = roller,
        )
    }
}

enum class NavAnsattRolle {
    TEAM_MULIGHETSROMMET,
    BETABRUKER,
    KONTAKTPERSON,
    UKJENT,
}
