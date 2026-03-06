package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID

data class OpprettUtbetalingAnnenAvtaltPris(
    val id: UUID,
    val gjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val journalpostId: JournalpostId?,
    val kommentar: String?,
    val korreksjonGjelderUtbetalingId: UUID?,
    val korreksjonBegrunnelse: String?,
    val kid: Kid?,
    val pris: ValutaBelop,
    val tilskuddstype: Tilskuddstype,
    val vedlegg: List<Vedlegg>,
)
