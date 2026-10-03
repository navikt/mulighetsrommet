package no.nav.mulighetsrommet.api.utbetaling.model

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures.utbetalingDto1
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.FieldError

class UtbetalingTest : FunSpec({
    test("enkeltplassutbetaling kan ikke settes til avbrytelse") {
        val utbetaling = utbetalingDto1.copy(
            gjennomforing = utbetalingDto1.gjennomforing.copy(type = GjennomforingType.ENKELTPLASS),
            status = UtbetalingStatusType.TIL_BEHANDLING,
        )

        utbetaling.kanSettesTilAvbrytelse() shouldBe false

        utbetaling.settTilAbrytelse(
            agent = NavAnsattFixture.DonaldDuck.navIdent,
            aarsaker = listOf(UtbetalingStatusAarsak.TILSAGN_GJORT_OPP.name),
            begrunnelse = null,
        ).shouldBeLeft(listOf(FieldError.of("Utbetaling for enkeltplass kan ikke avbrytes")))
    }
})
