package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregning
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class TilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val belopBrukt: Int,
    val belopUtbetalt: Int,
    val belopGjenstaende: Int,
    val kostnadssted: NavEnhetDbo,
    val beregning: TilsagnBeregning,
    val lopenummer: Int,
    val bestillingsnummer: String,
    val status: TilsagnStatus,
) {
    companion object {
        fun fromTilsagn(tilsagn: Tilsagn) = TilsagnDto(
            id = tilsagn.id,
            type = tilsagn.type,
            periode = tilsagn.periode,
            belopBrukt = tilsagn.belopBrukt,
            belopUtbetalt = tilsagn.belopUtbetalt,
            belopGjenstaende = tilsagn.gjenstaendeBelop(),
            kostnadssted = tilsagn.kostnadssted,
            beregning = tilsagn.beregning,
            lopenummer = tilsagn.lopenummer,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            status = tilsagn.status,
        )
    }
}
