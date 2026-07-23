package no.nav.mulighetsrommet.api.domain.deltaker

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent

@Serializable
data class NavVeileder(
    val navIdent: NavIdent,
    val enhetsnummer: NavEnhetNummer?,
)
