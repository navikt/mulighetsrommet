package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

data class TilskuddBehandlingKompakt(
    val id: UUID,
    val status: TilskuddBehandlingStatus,
    val type: TilskuddBehandlingType,
    val tilskuddVedtakId: UUID,
    val soknadDato: LocalDate,
    val journalpostId: JournalpostId,
    val tilskuddtype: Opplaeringtilskudd.Kode,
    val periode: Periode,
    val kostnadssted: KostnadsstedDto,
    val vedtakResultat: VedtakResultat,
)
