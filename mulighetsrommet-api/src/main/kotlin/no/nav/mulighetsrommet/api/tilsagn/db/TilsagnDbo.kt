package no.nav.mulighetsrommet.api.tilsagn.db

import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDateTime
import java.util.*

data class TilsagnDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val lopenummer: Int,
    val bestillingsnummer: String,
    val kostnadssted: String,
    val beregning: TilsagnBeregning,
    val arrangorId: UUID,
    val endretAv: NavIdent,
    val endretTidspunkt: LocalDateTime,
)
