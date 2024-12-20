package no.nav.mulighetsrommet.api.tilsagn.db

import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class TilsagnDbo(
    val id: UUID,
    val tiltaksgjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val kostnadssted: String,
    val beregning: TilsagnBeregning,
    val arrangorId: UUID,
    val endretAv: NavIdent,
    val endretTidspunkt: LocalDateTime,
    val type: TilsagnType,
)
