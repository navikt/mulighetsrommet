package no.nav.mulighetsrommet.api.navansatt

import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.NavEnhetNummer

data class OkonomiBeslutningContext(
    val gjennomforingType: GjennomforingType,
    val kostnadssted: NavEnhetNummer,
)
