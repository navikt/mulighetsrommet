package no.nav.mulighetsrommet.api.contracts.helved

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.contracts.test.assertContract
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class HelVedUtbetalingContractTest : FunSpec({
    test("utbetaling med alle felter") {
        assertContract(
            HelVedUtbetaling(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                sakId = "2026-123",
                behandlingId = "2026-123-1",
                personIdent = NorskIdent("01010199999"),
                periode = HelVedUtbetaling.Periode(
                    fom = LocalDate.of(2026, 3, 1),
                    tom = LocalDate.of(2026, 3, 31),
                ),
                belop = 12500,
                kostnadssted = NavEnhetNummer("0400"),
                tilskuddstype = HelVedUtbetaling.Tilskuddstype.SKOLEPENGER,
                tiltakskode = HelVedUtbetaling.Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
                saksbehandler = NavIdent("A123456"),
                beslutter = NavIdent("B654321"),
                besluttetTidspunkt = Instant.parse("2026-02-03T11:16:31.123456Z"),
                dryrun = false,
            ),
        )
    }
})
