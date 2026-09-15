package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate
import java.util.UUID

data class TilskuddVedtak(
    val id: UUID,
    val tilskuddId: UUID,
    val tilskuddOpplaeringType: Opplaeringtilskudd.Kode,
    val soknadJournalpostId: String,
    val soknadDato: LocalDate,
    val soknadBelop: ValutaBelop,
    val periode: Periode,
    val kostnadssted: NavEnhetNummer,
    val vedtakResultat: VedtakResultat,
    val utbetalingMottaker: TilskuddMottaker,
    val kid: Kid?,
    val utbetalingBelop: ValutaBelop?,
    val kommentarIntern: String?,
    val kommentarVedtaksbrev: String?,
)

enum class TilskuddMottaker {
    BRUKER,
    ARRANGOR,
}
