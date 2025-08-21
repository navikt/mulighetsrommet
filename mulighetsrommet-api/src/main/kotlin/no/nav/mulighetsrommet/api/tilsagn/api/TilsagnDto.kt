package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
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
    val belopGjenstaende: Int,
    val kostnadssted: KostnadsstedDto,
    // TODO: eksponere en forenklet beregningsmodell, tilsavarende UtbetalingDto?
    val beregning: TilsagnBeregningDto,
    val lopenummer: Int,
    val bestillingsnummer: String,
    val status: TilsagnStatus,
    val kommentar: String?,
) {
    companion object {
        fun fromTilsagn(tilsagn: Tilsagn) = TilsagnDto(
            id = tilsagn.id,
            type = tilsagn.type,
            periode = tilsagn.periode,
            belopBrukt = tilsagn.belopBrukt,
            belopGjenstaende = tilsagn.gjenstaendeBelop(),
            kostnadssted = KostnadsstedDto.fromNavEnhetDbo(tilsagn.kostnadssted),
            beregning = TilsagnBeregningDto.from(tilsagn.beregning),
            lopenummer = tilsagn.lopenummer,
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            status = tilsagn.status,
            kommentar = tilsagn.kommentar,
        )
    }
}
