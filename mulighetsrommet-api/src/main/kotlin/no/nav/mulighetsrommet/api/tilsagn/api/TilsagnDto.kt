package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val pris: ValutaBelop,
    val belopBrukt: ValutaBelop,
    val belopGjenstaende: ValutaBelop,
    val kostnadssted: KostnadsstedDto,
    val bestillingsnummer: String,
    val status: TilsagnStatusDto,
    val kommentar: String?,
    val beskrivelse: String?,
) {
    companion object {
        fun fromTilsagn(tilsagn: Tilsagn) = TilsagnDto(
            id = tilsagn.id,
            type = tilsagn.type,
            periode = tilsagn.periode,
            pris = tilsagn.beregning.output.pris,
            belopBrukt = tilsagn.belopBrukt,
            belopGjenstaende = tilsagn.gjenstaendeBelop(),
            kostnadssted = KostnadsstedDto.fromNavEnhetDbo(tilsagn.kostnadssted),
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            status = TilsagnStatusDto(tilsagn.status),
            kommentar = tilsagn.kommentar,
            beskrivelse = tilsagn.beskrivelse,
        )
    }
}

@Serializable
data class TilsagnStatusDto(
    val type: TilsagnStatus,
) {
    val status: DataElement.Status = toTilsagnStatusTag(type)
}
