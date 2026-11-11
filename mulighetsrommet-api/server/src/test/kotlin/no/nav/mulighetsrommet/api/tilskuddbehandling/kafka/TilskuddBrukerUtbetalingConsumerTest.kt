package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.brukerutbetaling.OpphorTilskuddUtbetaling
import no.nav.mulighetsrommet.api.brukerutbetaling.UtbetalTilskudd
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.util.UUID

class TilskuddBrukerUtbetalingConsumerTest : FunSpec({
    val service = mockk<BrukerUtbetalingService>(relaxed = true)
    val consumer = TilskuddBrukerUtbetalingConsumer(service)
    val behandlingId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val hendelse = TotrinnskontrollHendelse(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        entityId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
        type = TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
        status = TotrinnskontrollHendelse.Status.GODKJENT,
        behandletAv = TotrinnskontrollAgent.NavAnsatt(NavIdent("Z123456")),
        behandletTidspunkt = Instant.parse("2025-03-14T10:00:00Z"),
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavIdent("Z654321")),
        besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
    )

    beforeEach {
        clearMocks(service)
    }

    test("sender utbetalingskommando for godkjent registrering") {
        consumer.consume(behandlingId, hendelse)

        coVerify(exactly = 1) {
            service.utbetalTilskudd(
                UtbetalTilskudd(
                    behandlingId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    saksbehandler = NavIdent("Z123456"),
                    beslutter = NavIdent("Z654321"),
                    besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
                ),
            )
        }
        coVerify(exactly = 0) { service.opphorTilskuddUtbetaling(any()) }
    }

    test("sender opphørskommando for godkjent opphør") {
        consumer.consume(behandlingId, hendelse.copy(type = TotrinnskontrollType.TILSKUDD_OPPHOR))

        coVerify(exactly = 1) {
            service.opphorTilskuddUtbetaling(
                OpphorTilskuddUtbetaling(
                    behandlingId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    saksbehandler = NavIdent("Z123456"),
                    beslutter = NavIdent("Z654321"),
                    besluttetTidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
                ),
            )
        }
        coVerify(exactly = 0) { service.utbetalTilskudd(any()) }
    }

    test("ignorerer hendelser som ikke er godkjent") {
        consumer.consume(behandlingId, hendelse.copy(status = TotrinnskontrollHendelse.Status.TIL_BEHANDLING, besluttetAv = null))
        consumer.consume(behandlingId, hendelse.copy(status = TotrinnskontrollHendelse.Status.SATT_PA_VENT, besluttetAv = null))
        consumer.consume(behandlingId, hendelse.copy(status = TotrinnskontrollHendelse.Status.RETURNERT))

        coVerify { service wasNot Called }
    }

    test("ignorerer andre totrinnskontrolltyper") {
        consumer.consume(behandlingId, hendelse.copy(type = TotrinnskontrollType.TILSAGN_OPPRETTELSE))

        coVerify { service wasNot Called }
    }

    test("avviser godkjent hendelse uten beslutter") {
        shouldThrow<IllegalStateException> {
            consumer.consume(behandlingId, hendelse.copy(besluttetAv = null))
        }.message shouldBe "besluttetAv must be NavAnsatt"

        coVerify { service wasNot Called }
    }

    test("lar feil fra tjenesten propagere slik at hendelsen kan prøves på nytt") {
        coEvery { service.utbetalTilskudd(any()) } throws IllegalStateException("Utbetaling feilet")

        shouldThrow<IllegalStateException> {
            consumer.consume(behandlingId, hendelse)
        }.message shouldBe "Utbetaling feilet"
    }
})
