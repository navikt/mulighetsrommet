package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: AdminUtbetalingStatus,
    val periode: Periode,
    val beregning: Beregning,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val beskrivelse: String?,
    val innsendtAv: String?,
    val journalpostId: String?,
    val tilskuddstype: Tilskuddstype,
    val kostnadssteder: List<NavEnhetDbo>,
) {
    @Serializable
    data class Beregning(
        val belop: Int,
    )

    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: AdminUtbetalingStatus, kostnadssteder: List<NavEnhetDbo>?) = UtbetalingDto(
            id = utbetaling.id,
            status = status,
            periode = utbetaling.periode,
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            betalingsinformasjon = utbetaling.betalingsinformasjon,
            createdAt = utbetaling.createdAt,
            beskrivelse = utbetaling.beskrivelse,
            beregning = Beregning(
                belop = utbetaling.beregning.output.belop,
            ),
            innsendtAv = formaterInnsendtAv(utbetaling.innsender),
            journalpostId = utbetaling.journalpostId,
            tilskuddstype = utbetaling.tilskuddstype,
            kostnadssteder = kostnadssteder ?: emptyList(),
        )

        private fun formaterInnsendtAv(agent: Agent?): String? {
            return when (agent) {
                Arena -> "Arena"
                Arrangor -> "Arrangør"
                is NavIdent -> agent.value
                Tiltaksadministrasjon -> "System"
                else -> null
            }
        }
    }
}
