package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate
import java.util.UUID

object TilskuddFixtures {
    val Behandling = TilskuddBehandlingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = EnkelAmo.id,
        soknadJournalpostId = "J-2024-001",
        soknadDato = LocalDate.of(2024, 1, 15),
        periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1)),
        kostnadssted = NavEnhetFixtures.Innlandet.enhetsnummer,
        tilskudd = emptyList(),
        status = TilskuddBehandlingStatus.TIL_ATTESTERING,
        kommentarIntern = "kommentarIntern",
    )
    val Tilskudd = TilskuddDbo(
        id = UUID.randomUUID(),
        tilskuddOpplaeringType = TilskuddOpplaeringType.SKOLEPENGER,
        soknadBelop = ValutaBelop(
            belop = 100,
            valuta = Valuta.NOK,
        ),
        utbetalingBelop = ValutaBelop(
            belop = 100,
            valuta = Valuta.NOK,
        ),
        vedtakResultat = VedtakResultat.INNVILGELSE,
        kommentarVedtaksbrev = null,
        utbetalingMottaker = "Universitetet i Oslo",
        kid = Kid.parse("116"),
    )
}
