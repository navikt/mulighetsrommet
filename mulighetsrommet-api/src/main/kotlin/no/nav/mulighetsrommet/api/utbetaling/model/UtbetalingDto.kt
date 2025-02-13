package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.OkonomiPart.NavAnsatt
import java.time.LocalDateTime
import java.util.*

enum class Beregningsmodell {
    FORHANDSGODKJENT,
    FRI,
}

@Serializable
data class UtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val innsender: Innsender?,
    val status: UtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: Tiltakstype,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val beregning: UtbetalingBeregning,
    val betalingsinformasjon: Betalingsinformasjon,
    val journalpostId: String?,
    val periode: Periode,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val delutbetalinger: List<DelutbetalingDto>,
) {
    @Serializable
    data class Gjennomforing(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
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
    data class Tiltakstype(
        val navn: String,
    )

    @Serializable
    data class Betalingsinformasjon(
        val kontonummer: Kontonummer?,
        val kid: Kid?,
    )

    @Serializable
    sealed class Innsender {
        abstract val value: String

        @Serializable
        data class NavAnsatt(val navIdent: NavIdent) : Innsender() {
            override val value = navIdent.value
        }

        @Serializable
        data object ArrangorAnsatt : Innsender() {
            override val value = "ARRANGOR_ANSATT"
        }

        companion object {
            fun fromString(value: String): Innsender {
                return if (value == "ARRANGOR_ANSATT") {
                    ArrangorAnsatt
                } else {
                    NavAnsatt(NavIdent(value))
                }
            }
        }
    }
}
