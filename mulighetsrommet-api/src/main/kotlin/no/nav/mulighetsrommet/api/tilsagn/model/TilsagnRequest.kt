package no.nav.mulighetsrommet.api.tilsagn.model

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

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
    val beskrivelse: String? = null,
    val periodeStart: String? = null,
    val periodeSlutt: String? = null,
    val deltakere: List<TilsagnDeltakerRequest>? = null,
) {
    @OptIn(ExperimentalContracts::class)
    fun validate(): Either<List<FieldError>, TilsagnRequest> = validation {
        requireValid(id != null) {
            FieldError.of("Id er påkrevd", TilsagnRequest::id)
        }
        requireValid(!periodeStart.isNullOrBlank()) {
            FieldError.of("Periodestart er påkrevd", TilsagnRequest::periodeStart)
        }
        requireValid(!periodeSlutt.isNullOrBlank()) {
            FieldError.of("Periodeslutt er påkrevd", TilsagnRequest::periodeSlutt)
        }
        requireValid(kostnadssted != null) {
            FieldError.of("Kostnadssted er påkrevd", TilsagnRequest::kostnadssted)
        }
        validate((kommentar?.length ?: 0) <= 500) {
            FieldError.of("Kommentar kan ikke inneholde mer enn 500 tegn", TilsagnRequest::kommentar)
        }
        validate((beskrivelse?.length ?: 0) <= 250) {
            FieldError.of("Beskrivelse kan ikke inneholde mer enn 250 tegn", TilsagnRequest::beskrivelse)
        }
        this@TilsagnRequest
    }
}

@Serializable
data class TilsagnDeltakerRequest(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val innholdAnnet: String?,
)

@Serializable
data class TilsagnInputLinjeRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val beskrivelse: String? = null,
    val pris: ValutaBelop? = null,
    val antall: Int? = null,
)

@Serializable
data class TilsagnBeregningRequest(
    val type: TilsagnBeregningType,
    val valuta: Valuta? = null,
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
    val beregning: TilsagnBeregningDto?,
)
