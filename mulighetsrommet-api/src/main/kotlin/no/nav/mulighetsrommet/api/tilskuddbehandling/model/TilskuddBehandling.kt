package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.util.UUID

data class TilskuddBehandling(
    val id: UUID,
    val gjennomforingId: UUID,
    val soknadJournalpostId: String,
    val soknadDato: LocalDate,
    val periode: Periode,
    val kostnadssted: NavEnhetNummer,
    val vedtak: List<TilskuddVedtakDto>,
    val status: TilskuddBehandlingStatus,
)
