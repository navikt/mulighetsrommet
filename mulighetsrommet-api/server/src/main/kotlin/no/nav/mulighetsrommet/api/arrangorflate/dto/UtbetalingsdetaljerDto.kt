package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DataDetails

@Serializable
data class UtbetalingsdetaljerDto(
    val arrangorNavn: String,
    val tiltakstypeNavn: String,
    val innsending: DataDetails,
    val utbetaling: DataDetails,
    val satsDetaljer: List<DataDetails>,
    val betalingsinformasjon: DataDetails,
    val status: DataDetails,
    val avbrytelse: DataDetails?,
    val tilsagnHeader: String,
    val tilsagn: List<DataDetails>,
)
