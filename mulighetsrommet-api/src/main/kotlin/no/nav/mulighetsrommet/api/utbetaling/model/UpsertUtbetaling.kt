package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

sealed class UpsertUtbetaling {
    abstract val id: UUID

    data class Generering(
        override val id: UUID,
        val periode: Periode,
        val gjennomforingId: UUID,
        val beregning: UtbetalingBeregning,
        val tilskuddstype: Tilskuddstype,
        val kid: Kid?,
        val blokkeringer: Set<Utbetaling.Blokkering>,
    ) : UpsertUtbetaling()

    data class Innsending(
        override val id: UUID,
        val periode: Periode,
        val gjennomforingId: UUID,
        val beregning: UtbetalingBeregning,
        val tilskuddstype: Tilskuddstype,
        val kid: Kid?,
    ) : UpsertUtbetaling()

    data class Anskaffelse(
        override val id: UUID,
        val periode: Periode,
        val gjennomforingId: UUID,
        val beregning: UtbetalingBeregning,
        val tilskuddstype: Tilskuddstype,
        val kommentar: String?,
        val kid: Kid?,
        val journalpostId: JournalpostId?,
    ) : UpsertUtbetaling()

    data class Korreksjon(
        override val id: UUID,
        val periode: Periode,
        val beregning: UtbetalingBeregning,
        val tilskuddstype: Tilskuddstype,
        val korreksjonGjelderUtbetalingId: UUID,
        val korreksjonBegrunnelse: String,
        val kommentar: String?,
        val kid: Kid?,
    ) : UpsertUtbetaling()
}
