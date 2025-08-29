package no.nav.mulighetsrommet.api.tilsagn.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TilsagnRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val type: TilsagnType,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val kostnadssted: NavEnhetNummer? = null,
    val beregning: TilsagnBeregningRequest,
    val kommentar: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate? = null,
)

@Serializable
data class TilsagnInputLinjeRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val beskrivelse: String? = null,
    val belop: Int? = null,
    val antall: Int? = null,
)

@Serializable
data class TilsagnBeregningRequest(
    val type: TilsagnBeregningType,
    val antallPlasser: Int? = null,
    val prisbetingelser: String? = null,
    val linjer: List<TilsagnInputLinjeRequest>? = null,
    val antallTimerOppfolgingPerDeltaker: Int? = null,
)

@Serializable
data class BeregnTilsagnRequest(
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate? = null,
    val beregning: TilsagnBeregningRequest,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
)

@Serializable
data class BeregnTilsagnResponse(
    val success: Boolean,
    val beregning: TilsagnBeregningDto?,
)
