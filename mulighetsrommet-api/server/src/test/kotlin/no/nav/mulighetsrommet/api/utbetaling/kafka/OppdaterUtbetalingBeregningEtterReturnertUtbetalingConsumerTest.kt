package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class OppdaterUtbetalingBeregningEtterReturnertUtbetalingConsumerTest :
    FunSpec({
        val database = extension(ApiDatabaseTestListener())

        val gjennomforing = GjennomforingFixtures.AFT1
        val tilsagn = TilsagnFixtures.Tilsagn1.copy(gjennomforingId = gjennomforing.id)
        val utbetaling = UtbetalingFixtures.utbetaling1.copy(gjennomforingId = gjennomforing.id)
        val utbetalingLinje = UtbetalingFixtures.utbetalingLinje1.copy(
            utbetalingId = utbetaling.id,
            tilsagnId = tilsagn.id,
            periode = utbetaling.periode,
        )

        fun createConsumer(genererUtbetalingService: GenererUtbetalingService = mockk<GenererUtbetalingService>()): OppdaterUtbetalingBeregningEtterReturnertUtbetalingConsumer {
            return OppdaterUtbetalingBeregningEtterReturnertUtbetalingConsumer(
                db = database.api,
                genererUtbetalingService = genererUtbetalingService,
            )
        }

        fun opprettHendelseMedTypeOgStatus(type: TotrinnskontrollType, status: TotrinnskontrollHendelse.Status) = TotrinnskontrollHendelse(
            id = UUID.randomUUID(),
            entityId = utbetalingLinje.id,
            type = type,
            status = status,
            behandletAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.DonaldDuck.navIdent),
            behandletTidspunkt = Instant.now(),
            besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent),
            besluttetTidspunkt = Instant.now(),
            aarsaker = emptyList(),
            forklaring = null,
        )

        beforeEach {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.AFT),
                gjennomforinger = listOf(gjennomforing),
                tilsagn = listOf(tilsagn),
                utbetalinger = listOf(utbetaling),
                utbetalingLinjer = listOf(utbetalingLinje),
            ).initialize(database.api)
        }

        test("skal kun trigge oppdatering for hendelser av type UTBETALING_LINJE_OPPRETTELSE") {
            forAll(
                row(TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE, true),
                row(TotrinnskontrollType.TILSAGN_OPPRETTELSE, false),
                row(TotrinnskontrollType.TILSAGN_ANNULLERING, false),
                row(TotrinnskontrollType.TILSAGN_OPPGJOR, false),
                row(TotrinnskontrollType.UTBETALING_AVBRYTELSE, false),
                row(TotrinnskontrollType.ENKELTPLASS_OKONOMI, false),
                row(TotrinnskontrollType.ENKELTPLASS_PRISENDRING, false),
                row(TotrinnskontrollType.TILSKUDD_OPPRETTELSE, false),
            ) { type, shouldBeCalled ->
                val genererUtbetalingService = mockk<GenererUtbetalingService> {
                    every { skedulerOppdaterUtbetalingerForGjennomforing(any(), any()) } returns Unit
                }
                val consumer = createConsumer(genererUtbetalingService)

                val hendelse = opprettHendelseMedTypeOgStatus(type, TotrinnskontrollHendelse.Status.RETURNERT)
                consumer.consume(hendelse.entityId.toString(), hendelse)

                verify(exactly = if (shouldBeCalled) 1 else 0) {
                    genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(gjennomforing.id, any())
                }
            }
        }

        test("skal kun trigge oppdatering når status er RETURNERT") {
            forAll(
                row(TotrinnskontrollHendelse.Status.RETURNERT, true),
                row(TotrinnskontrollHendelse.Status.GODKJENT, false),
                row(TotrinnskontrollHendelse.Status.TIL_BEHANDLING, false),
                row(TotrinnskontrollHendelse.Status.SATT_PA_VENT, false),
            ) { status, shouldBeCalled ->
                val genererUtbetalingService = mockk<GenererUtbetalingService> {
                    every { skedulerOppdaterUtbetalingerForGjennomforing(any(), any()) } returns Unit
                }
                val consumer = createConsumer(genererUtbetalingService)

                val hendelse = opprettHendelseMedTypeOgStatus(TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE, status)
                consumer.consume(hendelse.entityId.toString(), hendelse)

                verify(exactly = if (shouldBeCalled) 1 else 0) {
                    genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(gjennomforing.id, any())
                }
            }
        }

        test("skal skedulere oppdatering av utbetalinger for gjennomføringen til den returnerte utbetalingslinjen") {
            val genererUtbetalingService = mockk<GenererUtbetalingService> {
                every { skedulerOppdaterUtbetalingerForGjennomforing(any(), any()) } returns Unit
            }
            val consumer = createConsumer(genererUtbetalingService)

            val forVentetTidspunkt = Instant.now()
            consumer.oppdaterUtbetalingBeregningFraReturnertLinje(utbetalingLinje.id)

            val tidspunkt = slot<Instant>()
            verify(exactly = 1) {
                genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(gjennomforing.id, capture(tidspunkt))
            }
            tidspunkt.captured.isAfter(forVentetTidspunkt.plusSeconds(4)) shouldBe true
            tidspunkt.captured.isBefore(forVentetTidspunkt.plus(6, ChronoUnit.SECONDS)) shouldBe true
        }
    })
