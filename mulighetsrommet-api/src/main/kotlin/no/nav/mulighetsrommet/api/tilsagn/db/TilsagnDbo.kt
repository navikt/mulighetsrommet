package no.nav.mulighetsrommet.api.tilsagn.db

import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.util.*

data class TilsagnDbo(
    val id: UUID,
    val tiltaksgjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val kostnadssted: String,
    val beregning: Prismodell.TilsagnBeregning,
    val opprettetAv: NavIdent,
    val arrangorId: UUID,
)
