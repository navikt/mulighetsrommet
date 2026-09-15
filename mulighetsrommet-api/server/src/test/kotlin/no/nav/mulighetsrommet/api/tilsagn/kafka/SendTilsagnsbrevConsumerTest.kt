package no.nav.mulighetsrommet.api.tilsagn.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.task.SendTilsagnsbrevSaga
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.Instant
import java.util.UUID

class SendTilsagnsbrevConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val enkeltplassGjennomforing = GjennomforingFixtures.EnkelAmo
    val avtaleGjennomforing = GjennomforingFixtures.ArbeidsrettetRehabilitering

    val enkeltplassTilsagn = TilsagnFixtures.createTilsagn(gjennomforingId = enkeltplassGjennomforing.id, lopenummer = 1)
    val avtaleTilsagn = TilsagnFixtures.createTilsagn(gjennomforingId = avtaleGjennomforing.id, lopenummer = 2)

    fun createConsumer(sendTilsagnsbrevSaga: SendTilsagnsbrevSaga = mockk(relaxed = true)): SendTilsagnsbrevConsumer {
        return SendTilsagnsbrevConsumer(
            db = database.api,
            sendTilsagnsbrevSaga = sendTilsagnsbrevSaga,
        )
    }

    fun opprettHendelseMedStatus(
        tilsagnId: UUID,
        type: TotrinnskontrollType,
        status: TotrinnskontrollHendelse.Status,
    ) = TotrinnskontrollHendelse(
        id = UUID.randomUUID(),
        entityId = tilsagnId,
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
            avtaler = listOf(AvtaleFixtures.ARR),
            gjennomforinger = listOf(enkeltplassGjennomforing, avtaleGjennomforing),
            tilsagn = listOf(enkeltplassTilsagn, avtaleTilsagn),
        ) {
            setTilsagnStatus(enkeltplassTilsagn, TilsagnStatus.GODKJENT)
            setTilsagnStatus(avtaleTilsagn, TilsagnStatus.GODKJENT)
        }.initialize(database.api)
    }

    test("skedulerer tilsagnsbrev når et godkjent tilsagn for en enkeltplass-gjennomføring blir opprettet") {
        val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
        val consumer = createConsumer(saga)

        val hendelse = opprettHendelseMedStatus(
            enkeltplassTilsagn.id,
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollHendelse.Status.GODKJENT,
        )
        consumer.consume(hendelse.entityId, hendelse)

        verify(exactly = 1) { saga.schedule(enkeltplassTilsagn.id, any()) }
    }

    test("skedulerer ikke tilsagnsbrev for tilsagn tilhørende en gjennomføring som ikke er enkeltplass") {
        val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
        val consumer = createConsumer(saga)

        val hendelse = opprettHendelseMedStatus(
            avtaleTilsagn.id,
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollHendelse.Status.GODKJENT,
        )
        consumer.consume(hendelse.entityId, hendelse)

        verify(exactly = 0) { saga.schedule(any(), any()) }
    }

    test("skedulerer ikke tilsagnsbrev dersom tilsagnet ikke lenger er godkjent (gammel hendelse)") {
        val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
        val consumer = createConsumer(saga)

        database.api.session {
            setTilsagnStatus(enkeltplassTilsagn, TilsagnStatus.RETURNERT)
        }

        val hendelse = opprettHendelseMedStatus(
            enkeltplassTilsagn.id,
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollHendelse.Status.GODKJENT,
        )
        consumer.consume(hendelse.entityId, hendelse)

        verify(exactly = 0) { saga.schedule(any(), any()) }
    }

    test("skedulerer ikke tilsagnsbrev når tilsagnet ikke finnes") {
        val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
        val consumer = createConsumer(saga)

        val hendelse = opprettHendelseMedStatus(
            UUID.randomUUID(),
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollHendelse.Status.GODKJENT,
        )
        consumer.consume(hendelse.entityId, hendelse)

        verify(exactly = 0) { saga.schedule(any(), any()) }
    }

    test("skedulerer bare tilsagnsbrev for TILSAGN_OPPRETTELSE og status GODKJENT") {
        forAll(
            row(TotrinnskontrollType.TILSAGN_OPPRETTELSE, TotrinnskontrollHendelse.Status.GODKJENT, true),
            row(TotrinnskontrollType.TILSAGN_OPPRETTELSE, TotrinnskontrollHendelse.Status.TIL_BEHANDLING, false),
            row(TotrinnskontrollType.TILSAGN_OPPRETTELSE, TotrinnskontrollHendelse.Status.RETURNERT, false),
            row(TotrinnskontrollType.TILSAGN_OPPRETTELSE, TotrinnskontrollHendelse.Status.SATT_PA_VENT, false),
            row(TotrinnskontrollType.TILSAGN_ANNULLERING, TotrinnskontrollHendelse.Status.GODKJENT, false),
            row(TotrinnskontrollType.TILSAGN_OPPGJOR, TotrinnskontrollHendelse.Status.GODKJENT, false),
            row(TotrinnskontrollType.UTBETALING_LINJE_OPPRETTELSE, TotrinnskontrollHendelse.Status.GODKJENT, false),
        ) { type, status, shouldBeCalled ->
            val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
            val consumer = createConsumer(saga)

            val hendelse = opprettHendelseMedStatus(enkeltplassTilsagn.id, type, status)
            consumer.consume(hendelse.entityId, hendelse)

            verify(exactly = if (shouldBeCalled) 1 else 0) { saga.schedule(enkeltplassTilsagn.id, any()) }
        }
    }
})
