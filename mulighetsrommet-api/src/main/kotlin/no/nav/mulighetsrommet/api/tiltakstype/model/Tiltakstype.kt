package no.nav.mulighetsrommet.api.tiltakstype.model

import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

data class Tiltakstype(
    val id: UUID,
    val navn: String,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val tiltakskode: Tiltakskode,
    val arenakode: String?,
    val sanityId: UUID?,
)
