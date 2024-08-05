package no.nav.mulighetsrommet.api.okonomi.tilsagn

import no.nav.mulighetsrommet.domain.dto.NavIdent
import java.time.LocalDate
import java.util.*

data class TilsagnDbo(
    val id: UUID,
    val tiltaksgjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val kostnadssted: String,
    val belop: Int,
    val opprettetAv: NavIdent,
    val arrangorId: UUID,
)
