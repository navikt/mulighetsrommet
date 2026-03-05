package no.nav.mulighetsrommet.api.tilsagn.db

import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.BestillingStatusType
import java.util.UUID

data class TilsagnDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val kostnadssted: NavEnhetNummer,
    val lopenummer: Int,
    val bestillingsnummer: String,
    val bestillingStatus: BestillingStatusType?,
    val belopBrukt: ValutaBelop,
    val beregning: TilsagnBeregning,
    val kommentar: String?,
    val beskrivelse: String?,
    val deltakere: List<UUID>,
)
