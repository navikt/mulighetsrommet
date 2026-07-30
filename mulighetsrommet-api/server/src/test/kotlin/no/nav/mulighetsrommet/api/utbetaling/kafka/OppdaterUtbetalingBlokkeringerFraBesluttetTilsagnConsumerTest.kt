package no.nav.mulighetsrommet.api.utbetaling.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.Instant
import java.util.UUID

class OppdaterUtbetalingBlokkeringerFraBesluttetTilsagnConsumerTest :
    FunSpec({
        val database = extension(ApiDatabaseTestListener())

        val gjennomforing = GjennomforingFixtures.ArbeidsrettetRehabilitering
        val tilsagn = TilsagnFixtures.Tilsagn1.copy(gjennomforingId = gjennomforing.id)

        fun createConsumer(genererUtbetalingService: GenererUtbetalingService = mockk<GenererUtbetalingService>()): OppdaterUtbetalingBlokkeringerFraBesluttetTilsagnConsumer {
            return OppdaterUtbetalingBlokkeringerFraBesluttetTilsagnConsumer(
                db = database.api,
                genererUtbetalingService = genererUtbetalingService,
            )
        }

        fun opprettHendelseMedStatus(type: TotrinnskontrollType, status: TotrinnskontrollHendelse.Status) = TotrinnskontrollHendelse(
            id = UUID.randomUUID(),
            entityId = tilsagn.id,
            type = type,
            status = status,
            behandletAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.DonaldDuck.navIdent),
            behandletTidspunkt = Instant.now(),
            besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent),
            besluttetTidspunkt = Instant.now(),
            aarsaker = emptyList(),
            forklaring = null,
        )

        fun opprettGodkjentHendelse(type: TotrinnskontrollType) = opprettHendelseMedStatus(type, TotrinnskontrollHendelse.Status.GODKJENT)

        beforeEach {
            MulighetsrommetTestDomain(
                ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
                avtaler = listOf(AvtaleFixtures.ARR),
                gjennomforinger = listOf(gjennomforing),
                tilsagn = listOf(tilsagn),
            ).initialize(database.api)
        }

        test("skal kalle oppdaterUtbetalingBlokkeringerForGjennomforing når tilsagn er besluttet") {
            forAll(
                row(TotrinnskontrollType.TILSAGN_OPPRETTELSE, true),
                row(TotrinnskontrollType.TILSAGN_ANNULLERING, true),
                row(TotrinnskontrollType.TILSAGN_OPPGJOR, true),
                row(TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE, false),
                row(TotrinnskontrollType.UTBETALING_AVBRYTELSE, false),
                row(TotrinnskontrollType.ENKELTPLASS_OKONOMI, false),
                row(TotrinnskontrollType.ENKELTPLASS_PRISENDRING, false),
                row(TotrinnskontrollType.TILSKUDD_OPPRETTELSE, false),
            ) { type, shouldBeCalled ->
                val genererUtbetalingService = mockk<GenererUtbetalingService> {
                    every { oppdaterUtbetalingBlokkeringerForGjennomforing(any()) } returns emptyList()
                }
                val consumer = createConsumer(genererUtbetalingService)

                val hendelse = opprettGodkjentHendelse(type)
                consumer.consume(hendelse.entityId.toString(), Json.encodeToJsonElement(hendelse))

                verify(exactly = if (shouldBeCalled) 1 else 0) {
                    genererUtbetalingService.oppdaterUtbetalingBlokkeringerForGjennomforing(gjennomforing.id)
                }
            }
        }

        test("skal ignorere hendelser som ikke er godkjent") {
            forAll(
                row(TotrinnskontrollHendelse.Status.GODKJENT, true),
                row(TotrinnskontrollHendelse.Status.TIL_BEHANDLING, false),
                row(TotrinnskontrollHendelse.Status.RETURNERT, false),
                row(TotrinnskontrollHendelse.Status.SATT_PA_VENT, false),
            ) { status, shouldBeCalled ->
                val genererUtbetalingService = mockk<GenererUtbetalingService> {
                    every { oppdaterUtbetalingBlokkeringerForGjennomforing(any()) } returns emptyList()
                }
                val consumer = createConsumer(genererUtbetalingService)

                val hendelse = opprettHendelseMedStatus(TotrinnskontrollType.TILSAGN_OPPRETTELSE, status)
                consumer.consume(hendelse.entityId.toString(), Json.encodeToJsonElement(hendelse))

                verify(exactly = if (shouldBeCalled) 1 else 0) {
                    genererUtbetalingService.oppdaterUtbetalingBlokkeringerForGjennomforing(gjennomforing.id)
                }
            }
        }
    })
