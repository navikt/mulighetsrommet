package no.nav.mulighetsrommet.api.gjennomforing.db

import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import java.time.LocalDate
import java.util.*

data class EnkeltplassDbo(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val amoKategorisering: AmoKategorisering?,
    val utdanningslop: UtdanningslopDbo?,
)
