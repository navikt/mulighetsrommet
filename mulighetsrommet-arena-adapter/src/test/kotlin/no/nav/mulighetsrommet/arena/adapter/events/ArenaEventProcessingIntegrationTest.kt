package no.nav.mulighetsrommet.arena.adapter.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.respondOk
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.clients.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.databaseConfig
import no.nav.mulighetsrommet.arena.adapter.events.processors.TiltakgjennomforingEventProcessor
import no.nav.mulighetsrommet.arena.adapter.events.processors.TiltakshistorikkEventProcessor
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaHistTiltakdeltakerEvent
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakdeltakerEvent
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakgjennomforingEvent
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Update
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Processed
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.SakRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEventService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.TiltakshistorikkClient

class ArenaEventProcessingIntegrationTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("replikering til tiltakshistorikk") {
        test("leverer ignorerte deltakere når gjennomføringen korrigeres") {
            val fixture = ArenaEventProcessingFixture(database.db)
            val gjennomforing = TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell.copy(SAK_ID = 1)
            val ignoredGjennomforing = createArenaTiltakgjennomforingEvent(Insert, gjennomforing.copy(LOKALTNAVN = null))
            val correctedGjennomforing = createArenaTiltakgjennomforingEvent(Update, gjennomforing)
            val deltaker = createArenaTiltakdeltakerEvent(Insert) {
                it.copy(TILTAKDELTAKER_ID = 11, TILTAKGJENNOMFORING_ID = 3780431)
            }
            val histDeltaker = createArenaHistTiltakdeltakerEvent(Insert) {
                it.copy(HIST_TILTAKDELTAKER_ID = 12, TILTAKGJENNOMFORING_ID = 3780431)
            }

            fixture.service.processEvent(ignoredGjennomforing)
            fixture.service.processEvent(deltaker)
            fixture.service.processEvent(histDeltaker)

            fixture.deliveredParticipants.shouldBeEmpty()
            listOf(deltaker, histDeltaker).forEach { child ->
                fixture.mappings.get(child.arenaTable, child.arenaId)?.status shouldBe Ignored
                fixture.events.get(child.arenaTable, child.arenaId)?.status shouldBe Processed
            }

            fixture.service.processEvent(correctedGjennomforing)

            fixture.mappings.get(ArenaTable.Tiltaksgjennomforing, "3780431")?.status shouldBe Handled
            fixture.events.get(ArenaTable.Tiltaksgjennomforing, "3780431")?.status shouldBe Processed
            fixture.deliveredParticipants.map { it.arenaDeltakerId }.shouldContainExactlyInAnyOrder(11, 12)
            listOf(deltaker, histDeltaker).forEach { child ->
                fixture.mappings.get(child.arenaTable, child.arenaId)?.status shouldBe Handled
                fixture.events.get(child.arenaTable, child.arenaId)?.status shouldBe Processed
            }
        }
    }
})

private class ArenaEventProcessingFixture(db: Database) {
    val events = ArenaEventRepository(db)
    val mappings = ArenaEntityMappingRepository(db)
    private val entities = ArenaEntityService(
        mappings,
        TiltakstypeRepository(db),
        SakRepository(db),
        TiltaksgjennomforingRepository(db),
    )
    val deliveredParticipants = mutableListOf<TiltakshistorikkArenaDeltaker>()
    private val engine = createMockEngine {
        get("/ords/arbeidsgiver") {
            respondJson(ArenaOrdsArrangor("123456789", "123456789"))
        }
        get("/ords/fnr") {
            respondJson(ArenaOrdsFnr("12345678910"))
        }
        put("/api/v1/intern/arena/gjennomforing") { respondOk() }
        put("/api/v1/intern/arena/deltaker") { request ->
            deliveredParticipants.add(request.decodeRequestBody<TiltakshistorikkArenaDeltaker>())
            respondOk()
        }
    }
    private val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") { "token" }
    private val api = MulighetsrommetApiClient(engine, baseUri = "http://mr-api") { "token" }
    private val historikk = TiltakshistorikkClient(engine, baseUrl = "http://tiltakshistorikk") { "token" }
    val service = ArenaEventService(
        events = events,
        processors = listOf(
            TiltakgjennomforingEventProcessor(
                TiltakgjennomforingEventProcessor.Config(),
                entities,
                ords,
                api,
                historikk,
            ),
            TiltakshistorikkEventProcessor(entities, historikk, ords),
        ),
        entities = entities,
    )

    init {
        val tiltakstype = TiltakstypeFixtures.Individuell
        TiltakstypeRepository(db).upsert(tiltakstype).getOrThrow()
        SakRepository(db).upsert(Sak(sakId = 1, lopenummer = 1, aar = 2022, enhet = "2990")).getOrThrow()
        mappings.upsert(
            ArenaEntityMapping(ArenaTable.Tiltakstype, "AMO", tiltakstype.id, Handled),
        )
    }
}
