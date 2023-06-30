package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.NavVeilederDto
import java.util.*

class NavVeilederService(
    private val microsoftGraphService: MicrosoftGraphService,
) {
    suspend fun getNavVeileder(azureId: UUID, oboToken: String): NavVeilederDto {
        val ansatt = microsoftGraphService.getNavAnsatt(azureId, oboToken)
        return NavVeilederDto(
            navIdent = ansatt.navIdent,
            fornavn = ansatt.fornavn,
            etternavn = ansatt.etternavn,
            hovedenhet = NavVeilederDto.Hovedenhet(
                enhetsnummer = ansatt.hovedenhetKode,
                navn = ansatt.hovedenhetNavn,
            ),
        )
    }
}
