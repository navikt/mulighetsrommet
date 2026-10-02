package no.nav.mulighetsrommet.api.navansatt

import arrow.core.NonEmptySet
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.NavEnhetNummer

data class OkonomiBeslutningContext(
    val gjennomforingType: GjennomforingType,
    val kostnadssteder: NonEmptySet<NavEnhetNummer>,
)
