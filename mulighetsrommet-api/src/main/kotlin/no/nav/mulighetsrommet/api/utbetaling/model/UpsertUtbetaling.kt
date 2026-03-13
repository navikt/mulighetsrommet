package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

sealed class UpsertUtbetaling {
    abstract val id: UUID

    data class Anskaffelse(
        override val id: UUID,
        val periode: Periode,
        val gjennomforingId: UUID,
        val beregning: UtbetalingBeregning,
        val tilskuddstype: Tilskuddstype,
        val kommentar: String?,
        val kid: Kid?,
        // TODO: journalpostId burde ikke være nullable, i stedet burde utbetaling opprettes _etter_ at innsending er arkivert?
        //  Da trenger ikke vedlegg være en del av denne modellen og det styres fra ArrangorflateUtbetalingService i stedet
        val journalpostId: JournalpostId?,
        val vedlegg: List<Vedlegg>,
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
