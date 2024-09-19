package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.NavVeilederDto
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.*

class NavVeilederService(
    private val microsoftGraphService: MicrosoftGraphService,
) {
    suspend fun getNavVeileder(azureId: UUID, obo: AccessType.OBO): NavVeilederDto {
        val ansatt = microsoftGraphService.getNavAnsatt(azureId, obo)
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
