package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaAvtaleInfoEvent
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalestatuskode
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Avtale
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

class AvtaleInfoEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when dependent tiltakstype is missing") {
            val consumer = createConsumer(database.db)

            val result = consumer.handleEvent(createArenaAvtaleInfoEvent(Insert))

            result.shouldBeLeft().should { it.status shouldBe Failed }
            database.assertThat("avtale").isEmpty
        }
    }

    context("when dependent tiltakstype has been processed") {
        val tiltakstype = TiltakstypeFixtures.Gruppe

        beforeEach {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype)

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.upsert(ArenaEntityMapping(ArenaTable.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id, Handled))
        }

        test("ignore avtaler when required fields are missing") {
            val consumer = createConsumer(database.db)

            val events = listOf(
                createArenaAvtaleInfoEvent(Insert) {
                    it.copy(DATO_FRA = null)
                },
                createArenaAvtaleInfoEvent(Insert) {
                    it.copy(DATO_TIL = null)
                },
                createArenaAvtaleInfoEvent(Insert) {
                    it.copy(ARBGIV_ID_LEVERANDOR = null)
                },
            )

            events.forEach { event ->
                consumer.handleEvent(event).shouldBeRight().should { it.status shouldBe Ignored }
            }
            database.assertThat("avtale").isEmpty
        }

        test("ignore avtaler ended before 2023") {
            val consumer = createConsumer(database.db)

            val event = createArenaAvtaleInfoEvent(Insert) {
                it.copy(DATO_TIL = "2022-12-31 00:00:00")
            }

            consumer.handleEvent(event).shouldBeRight().should { it.status shouldBe Ignored }
            database.assertThat("avtale").isEmpty
        }

        test("should treat all operations as upserts") {
            val engine = createMockEngine(
                "/ords/arbeidsgiver" to {
                    respondJson(ArenaOrdsArrangor("123456", "1000000"))
                },
                "/api/v1/internal/arena/avtale.*" to { respondOk() }
            )
            val consumer = createConsumer(database.db, engine)
            val entities = ArenaEntityMappingRepository(database.db)

            val e1 = createArenaAvtaleInfoEvent(Insert)
            entities.upsert(ArenaEntityMapping(e1.arenaTable, e1.arenaId, UUID.randomUUID(), ArenaEntityMapping.Status.Unhandled))
            consumer.handleEvent(e1) shouldBeRight ProcessingResult(Handled)
            database.assertThat("avtale").row().value("status").isEqualTo(Avtale.Status.Aktiv.name)

            val e2 = createArenaAvtaleInfoEvent(Update) {
                it.copy(AVTALESTATUSKODE = Avtalestatuskode.Planlagt)
            }
            consumer.handleEvent(e2) shouldBeRight ProcessingResult(Handled)
            database.assertThat("avtale").row().value("status").isEqualTo(Avtale.Status.Planlagt.name)

            val e3 = createArenaAvtaleInfoEvent(Update) {
                it.copy(AVTALESTATUSKODE = Avtalestatuskode.Avsluttet)
            }
            consumer.handleEvent(e3) shouldBeRight ProcessingResult(Handled)
            database.assertThat("avtale").row().value("status").isEqualTo(Avtale.Status.Avsluttet.name)
        }

        context("api responses") {
            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.InternalServerError)
                    }
                )

                val consumer = createConsumer(database.db, engine)
                val result = consumer.handleEvent(createArenaAvtaleInfoEvent(Insert))

                result.shouldBeLeft().should { it.status shouldBe Failed }
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.NotFound)
                    }
                )
                val entities = ArenaEntityMappingRepository(database.db)
                val event = createArenaAvtaleInfoEvent(Insert)
                entities.upsert(ArenaEntityMapping(event.arenaTable, event.arenaId, UUID.randomUUID(), ArenaEntityMapping.Status.Unhandled))
                val consumer = createConsumer(database.db, engine)
                val result = consumer.handleEvent(event)

                result.shouldBeLeft().should { it.status shouldBe Invalid }
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "100000")
                        )
                    },
                    "/api/v1/internal/arena/avtale" to {
                        respondError(HttpStatusCode.InternalServerError)
                    }
                )

                val consumer = createConsumer(database.db, engine)
                val result = consumer.handleEvent(createArenaAvtaleInfoEvent(Insert))

                result.shouldBeLeft().should { it.status shouldBe Failed }
            }

            test("should call api with mapped event payload when all services responds with success") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(ArenaOrdsArrangor("123456", "1000000"))
                    },
                    "/api/v1/internal/arena/avtale.*" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)
                val entities = ArenaEntityMappingRepository(database.db)

                val event = createArenaAvtaleInfoEvent(Insert)
                entities.upsert(ArenaEntityMapping(event.arenaTable, event.arenaId, UUID.randomUUID(), ArenaEntityMapping.Status.Unhandled))
                consumer.handleEvent(event).shouldBeRight()

                val generatedId = engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    val avtale = decodeRequestBody<AvtaleDbo>().apply {
                        tiltakstypeId shouldBe tiltakstype.id
                        avtalenummer shouldBe "2022#2000"
                        leverandorOrganisasjonsnummer shouldBe "1000000"
                        avtaletype shouldBe Avtaletype.Rammeavtale
                        avslutningsstatus shouldBe Avslutningsstatus.IKKE_AVSLUTTET
                    }

                    avtale.id
                }

                consumer.handleEvent(createArenaAvtaleInfoEvent(Delete)).shouldBeRight()

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    url.getLastPathParameterAsUUID() shouldBe generatedId
                }
            }
        }
    }
})

private fun createConsumer(
    db: Database,
    engine: HttpClientEngine = MockEngine { respondOk() }
): AvtaleInfoEventProcessor {
    val client = MulighetsrommetApiClient(engine, baseUri = "api") {
        "Bearer token"
    }

    val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
        "Bearer token"
    }
    val entities = ArenaEntityService(
        events = ArenaEventRepository(db),
        mappings = ArenaEntityMappingRepository(db),
        tiltakstyper = TiltakstypeRepository(db),
        saker = SakRepository(db),
        tiltaksgjennomforinger = TiltaksgjennomforingRepository(db),
        deltakere = DeltakerRepository(db),
        avtaler = AvtaleRepository(db),
    )

    return AvtaleInfoEventProcessor(entities, client, ords)
}
