package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.ArenaDeltakerDbo
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.clients.TiltakshistorikkClient
import no.nav.mulighetsrommet.arena.adapter.databaseConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaHistTiltakdeltakerEvent
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakdeltakerEvent
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Delete
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    context("handleEvent") {
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            avtaler = AvtaleRepository(database.db),
        )

        fun createProcessor(engine: HttpClientEngine = createMockEngine()): TiltakshistorikkEventProcessor {
            val client = TiltakshistorikkClient(engine, baseUri = "") {
                "Bearer token"
            }

            val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
                "Bearer token"
            }

            return TiltakshistorikkEventProcessor(entities, client, ords)
        }

        fun prepareEvent(
            event: ArenaEvent,
            status: ArenaEntityMapping.Status? = null,
        ): Pair<ArenaEvent, ArenaEntityMapping> {
            val mapping = entities.getOrCreateMapping(event)
            if (status != null) {
                entities.upsertMapping(mapping.copy(status = status))
            }
            return Pair(event, mapping)
        }

        context("when dependent events has not been processed") {
            test("should save the event with status Failed when the dependent tiltaksgjennomføring is missing") {
                val processor = createProcessor()

                val (event) = prepareEvent(createArenaTiltakdeltakerEvent(Insert))
                val result = processor.handleEvent(event)

                result.shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "ArenaEntityMapping mangler for arenaTable=Tiltaksgjennomforing og arenaId=3"
                }
            }
        }

        context("when dependent events has been processed") {
            val tiltakstypeIndividuell = TiltakstypeFixtures.Individuell
            val sakIndividuell = Sak(sakId = 2, lopenummer = 2, aar = 2022, enhet = "2990")
            val tiltaksgjennomforingIndividuell = Tiltaksgjennomforing(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = 3,
                sanityId = null,
                sakId = 2,
                tiltakskode = "AMO",
                arrangorId = 123456789,
                navn = "Gjennomføring individuell",
                status = "GJENNOMFOR",
                fraDato = LocalDateTime.of(2023, 1, 1, 0, 0),
                tilDato = null,
                apentForInnsok = true,
                antallPlasser = null,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            beforeEach {
                val mappings = ArenaEntityMappingRepository(database.db)

                val tiltakstyper = TiltakstypeRepository(database.db)
                listOf(tiltakstypeIndividuell).forEach {
                    tiltakstyper.upsert(it).getOrThrow()
                    mappings.upsert(
                        ArenaEntityMapping(ArenaTable.Tiltakstype, it.tiltakskode, it.id, Handled),
                    )
                }

                val saker = SakRepository(database.db)
                listOf(sakIndividuell).forEach {
                    saker.upsert(it).getOrThrow()
                }

                val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
                listOf(tiltaksgjennomforingIndividuell).forEach {
                    tiltaksgjennomforinger.upsert(it).getOrThrow()
                    mappings.upsert(
                        ArenaEntityMapping(
                            ArenaTable.Tiltaksgjennomforing,
                            it.tiltaksgjennomforingId.toString(),
                            it.id,
                            Handled,
                        ),
                    )
                }
            }

            test("should upsert tiltakshistorikk for ArenaDeltaker events") {
                val (deltakerEvent, mapping) = prepareEvent(createArenaTiltakdeltakerEvent(Insert), Ignored)

                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(ArenaOrdsArrangor("123456789", "000000000"))
                    }
                    get("/ords/fnr") {
                        respondJson(ArenaOrdsFnr("12345678910"))
                    }
                    put("/api/v1/intern/arena/deltaker") { respondOk() }
                    delete("/api/v1/intern/arena/deltaker/${mapping.entityId}") { respondOk() }
                }
                val processor = createProcessor(engine)

                processor.handleEvent(deltakerEvent).shouldBeRight().should { it.status shouldBe Handled }

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaDeltakerDbo>().apply {
                        id shouldBe mapping.entityId
                        norskIdent shouldBe NorskIdent("12345678910")
                    }
                }

                processor.handleEvent(createArenaTiltakdeltakerEvent(Delete)).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Delete
                }
            }

            test("should upsert tiltakshistorikk for ArenaHistDeltaker events") {
                val (histDeltakerEvent, histDeltakerMapping) = prepareEvent(
                    createArenaHistTiltakdeltakerEvent(Insert),
                    Ignored,
                )

                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(ArenaOrdsArrangor("123456789", "000000000"))
                    }
                    get("/ords/fnr") {
                        respondJson(ArenaOrdsFnr("12345678910"))
                    }
                    put("/api/v1/intern/arena/deltaker") { respondOk() }
                }
                val processor = createProcessor(engine)

                processor.handleEvent(histDeltakerEvent).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaDeltakerDbo>().apply {
                        id shouldBe histDeltakerMapping.entityId
                        norskIdent shouldBe NorskIdent("12345678910")
                    }
                }
            }
        }
    }
})
