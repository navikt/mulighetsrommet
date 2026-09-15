package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate
import java.util.UUID

data class Tilskudd(
    val id: UUID,
    val type: Opplaeringtilskudd,
    val gjennomforingId: UUID,
    val tilskuddsnummer: String,
    val vedtak: List<Vedtak>,
) {
    data class Vedtak(
        val id: UUID,
        val behandlingId: UUID,
        val soknadJournalpostId: String,
        val soknadDato: LocalDate,
        val periode: Periode,
        val kostnadssted: NavEnhetNummer,
        val soknadBelop: ValutaBelop,
        val utbetalingBelop: ValutaBelop?,
        val vedtakResultat: VedtakResultat,
        val kommentarVedtaksbrev: String?,
        val utbetalingMottaker: TilskuddMottaker,
        val kid: Kid?,
        val kommentarIntern: String?,
        val vedtakJournalpostId: String?,
    )
}

data class TilskuddKompakt(
    val id: UUID,
    val type: Opplaeringtilskudd,
    val gjennomforingId: UUID,
    val tilskuddsnummer: String,
    val sisteVedtakResultat: VedtakResultat?,
    val periode: Periode?,
    val sisteVedtakLopenummer: Int?
)
