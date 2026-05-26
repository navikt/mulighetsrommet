package no.nav.mulighetsrommet.api.tilsagn.model

import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import java.util.UUID

data class UpsertTilsagn(
    val id: UUID,
    val gjennomforingId: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val kostnadssted: NavEnhetNummer,
    val beregning: TilsagnBeregning,
    val kommentar: String?,
    val beskrivelse: String?,
    val deltakere: List<Deltaker>?,
) {
    data class Deltaker(
        val deltakerId: UUID,
        val innholdAnnet: String?,
    )
}
