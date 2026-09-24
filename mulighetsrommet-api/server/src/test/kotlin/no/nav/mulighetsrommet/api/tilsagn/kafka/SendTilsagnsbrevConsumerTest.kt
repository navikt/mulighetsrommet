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
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
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

    val avtaleGjennomforing = GjennomforingFixtures.ArbeidsrettetRehabilitering
    val enkeltplassAnskaffelse = GjennomforingFixtures.EnkelAmo
    val enkeltplassTilskudd = GjennomforingFixtures.EnkelAmo.copy(
        id = UUID.randomUUID(),
        prismodellId = PrismodellFixtures.TilskuddTilOpplaering.id,
    )

    val avtaleTilsagn = TilsagnFixtures.createTilsagn(gjennomforingId = avtaleGjennomforing.id, lopenummer = 2)
    val anskaffelseTilsagn = TilsagnFixtures.createTilsagn(gjennomforingId = enkeltplassAnskaffelse.id, lopenummer = 1)
    val tilskuddTilsagn = TilsagnFixtures.createTilsagn(gjennomforingId = enkeltplassTilskudd.id, lopenummer = 3)

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
        behandletBegrunnelse = null,
        behandletAarsaker = emptyList(),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavAnsattFixture.MikkeMus.navIdent),
        besluttetTidspunkt = Instant.now(),
        besluttetBegrunnelse = null,
        besluttetAarsaker = emptyList(),
    )

    beforeEach {
        MulighetsrommetTestDomain(
            ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
            avtaler = listOf(AvtaleFixtures.ARR),
            prismodeller = listOf(
                PrismodellFixtures.AnnenAvtaltPris,
                PrismodellFixtures.AnskaffetEnkeltplass,
                PrismodellFixtures.TilskuddTilOpplaering,
            ),
            gjennomforinger = listOf(avtaleGjennomforing, enkeltplassAnskaffelse, enkeltplassTilskudd),
            tilsagn = listOf(avtaleTilsagn, anskaffelseTilsagn, tilskuddTilsagn),
        ) {
            setTilsagnStatus(avtaleTilsagn, TilsagnStatus.GODKJENT)
            setTilsagnStatus(anskaffelseTilsagn, TilsagnStatus.GODKJENT)
            setTilsagnStatus(tilskuddTilsagn, TilsagnStatus.GODKJENT)
        }.initialize(database.api)
    }

    test("skedulerer tilsagnsbrev når et godkjent tilsagn for en enkeltplass-gjennomføring blir opprettet") {
        val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
        val consumer = createConsumer(saga)

        val hendelse = opprettHendelseMedStatus(
            anskaffelseTilsagn.id,
            TotrinnskontrollType.TILSAGN_OPPRETTELSE,
            TotrinnskontrollHendelse.Status.GODKJENT,
        )
        consumer.consume(hendelse.entityId, hendelse)

        verify(exactly = 1) { saga.schedule(anskaffelseTilsagn.id, any()) }
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

    test("skedulerer ikke tilsagnsbrev for enkeltplass-gjennomføring med annen prismodell enn anskaffet enkeltplass") {
        val saga = mockk<SendTilsagnsbrevSaga>(relaxed = true)
        val consumer = createConsumer(saga)

        val hendelse = opprettHendelseMedStatus(
            tilskuddTilsagn.id,
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
            setTilsagnStatus(anskaffelseTilsagn, TilsagnStatus.RETURNERT)
        }

        val hendelse = opprettHendelseMedStatus(
            anskaffelseTilsagn.id,
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

            val hendelse = opprettHendelseMedStatus(anskaffelseTilsagn.id, type, status)
            consumer.consume(hendelse.entityId, hendelse)

            verify(exactly = if (shouldBeCalled) 1 else 0) { saga.schedule(anskaffelseTilsagn.id, any()) }
        }
    }
})
