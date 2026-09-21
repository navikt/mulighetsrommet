package no.nav.mulighetsrommet.admin.navansatt

import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.UUID

data class EntraGroupNavAnsattRolleMapping(
    val entraGroupId: UUID,
    val rolle: Rolle,
    /**
     * Noen roller tildeles per kostnadssted. Et kostnadssted er et firesifret Nav-enhetsnummer.
     * Applikasjonen har definert alle kjente kostnadssteder og hvilken "region" de er tilknyttet.
     * Hvis en rolle er knyttet mot regionen til kostnadsstedet vil den ansatte også få tildelt
     * rollen til alle kostnadssteder under denne regionen.
     */
    val kostnadssteder: Set<NavEnhetNummer> = setOf(),
    val kommentar: String? = null,
)
