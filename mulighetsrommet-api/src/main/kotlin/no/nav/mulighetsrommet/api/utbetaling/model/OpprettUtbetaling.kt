package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID

data class OpprettUtbetaling(
    val gjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val kontonummer: Kontonummer,
    val kidNummer: Kid?,
    val pris: ValutaBelop,
    val vedlegg: List<Vedlegg>,
)

data class OpprettUtbetalingAnnenAvtaltPris(
    val id: UUID,
    val gjennomforingId: UUID,
    val periodeStart: LocalDate,
    val periodeSlutt: LocalDate,
    val journalpostId: JournalpostId?,
    val kommentar: String?,
    val korrigererUtbetaling: UUID?,
    val beskrivelse: String?,
    val kid: Kid?,
    val pris: ValutaBelop,
    val tilskuddstype: Tilskuddstype,
    val vedlegg: List<Vedlegg>,
)

fun OpprettUtbetaling.toAnnenAvtaltPris(
    gjennomforingId: UUID,
    tilskuddstype: Tilskuddstype,
): OpprettUtbetalingAnnenAvtaltPris {
    return OpprettUtbetalingAnnenAvtaltPris(
        id = UUID.randomUUID(),
        gjennomforingId = gjennomforingId,
        tilskuddstype = tilskuddstype,
        periodeStart = periodeStart,
        periodeSlutt = periodeSlutt,
        journalpostId = null,
        kommentar = null,
        korrigererUtbetaling = null,
        beskrivelse = null,
        kid = kidNummer,
        pris = pris,
        vedlegg = vedlegg,
    )
}
