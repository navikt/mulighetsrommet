package no.nav.mulighetsrommet.api.avtale.db

import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer

data class VeilederinformasjonDbo(
    val redaksjoneltInnhold: RedaksjoneltInnholdDbo?,
    val navEnheter: Set<NavEnhetNummer>,
)

data class RedaksjoneltInnholdDbo(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
)
