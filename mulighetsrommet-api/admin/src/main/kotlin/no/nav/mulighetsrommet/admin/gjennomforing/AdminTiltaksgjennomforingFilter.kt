package no.nav.mulighetsrommet.admin.gjennomforing

import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

data class AdminTiltaksgjennomforingFilter(
    val search: String? = null,
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val tiltakskoder: List<Tiltakskode> = emptyList(),
    val statuser: List<GjennomforingStatusType> = emptyList(),
    val sortering: String? = null,
    val avtaleId: UUID? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val publisert: Boolean? = null,
    val koordinatorNavIdent: NavIdent? = null,
)
