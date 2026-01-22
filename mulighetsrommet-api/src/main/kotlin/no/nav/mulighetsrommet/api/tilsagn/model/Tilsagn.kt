package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.minus
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.BestillingStatusType
import java.util.UUID

@Serializable
data class Tilsagn(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val belopBrukt: ValutaBelop,
    val kostnadssted: NavEnhetDbo,
    val beregning: TilsagnBeregning,
    val lopenummer: Int,
    val bestilling: Bestilling,
    val tiltakstype: Tiltakstype,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val status: TilsagnStatus,
    val kommentar: String?,
    val beskrivelse: String?,
) {
    @Serializable
    data class Tiltakstype(
        val tiltakskode: Tiltakskode,
        val navn: String,
    )

    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val lopenummer: Tiltaksnummer,
        val navn: String,
    )

    @Serializable
    data class Arrangor(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class Bestilling(
        val bestillingsnummer: String,
        val status: BestillingStatusType?,
    )

    fun gjenstaendeBelop(): ValutaBelop = if (status in listOf(TilsagnStatus.ANNULLERT, TilsagnStatus.OPPGJORT)) {
        0.withValuta(belopBrukt.valuta)
    } else {
        beregning.output.pris - belopBrukt
    }

    fun getTiltaksnavn(): String {
        return "${tiltakstype.navn} (${gjennomforing.lopenummer.value})"
    }
}
