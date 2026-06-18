package no.nav.mulighetsrommet.api.veilederflate.db

import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.util.*

data class DeltakelseTiltaksgjennomforing(
    val id: UUID,
    val type: GjennomforingType,
    val status: GjennomforingStatusType,
)
