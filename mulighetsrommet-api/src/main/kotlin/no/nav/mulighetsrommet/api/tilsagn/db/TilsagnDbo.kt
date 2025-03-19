package no.nav.mulighetsrommet.api.tilsagn.db

import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.BestillingStatusType
import java.util.*

data class TilsagnDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val kostnadssted: String,
    val lopenummer: Int,
    val bestillingsnummer: String,
    val bestillingstatus: BestillingStatusType?,
    val beregning: TilsagnBeregning,
)
