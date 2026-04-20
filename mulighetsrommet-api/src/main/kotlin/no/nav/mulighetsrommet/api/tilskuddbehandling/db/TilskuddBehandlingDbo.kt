package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

data class TilskuddBehandlingDbo(
    val id: UUID,
    val gjennomforingId: UUID,
    val soknadJournalpostId: String,
    val soknadDato: LocalDate,
    val periode: Periode,
    val kostnadssted: NavEnhetNummer,
    val vedtak: List<TilskuddVedtakDbo>,
    val status: TilskuddBehandlingStatus,
)
