package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.domain.dto.NavIdent
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
    val roller: Set<NavAnsattRolle>,
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
            roller = dto.roller,
            skalSlettesDato = dto.skalSlettesDato,
        )
    }
}

enum class NavAnsattRolle {
    TEAM_MULIGHETSROMMET,
    KONTAKTPERSON,
    TILTAKSGJENNOMFORINGER_SKRIV,
    AVTALER_SKRIV,
    TILTAKADMINISTRASJON_GENERELL,
    TILTAKADMINISTRASJON_ENDRINGSMELDING,
    OKONOMI_BESLUTTER,
}
