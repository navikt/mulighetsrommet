package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaAvtaleInfoEvent
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalestatuskode
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Avtale
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson

class AvtaleInfoEventProcessorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when dependent tiltakstype is missing") {
            val consumer = createConsumer(database.db)

            val event = consumer.processEvent(createArenaAvtaleInfoEvent(Insert))

            event.status shouldBe ArenaEvent.ProcessingStatus.Failed
            database.assertThat("avtale").isEmpty
        }
    }

    context("when dependent tiltakstype has been processed") {
        val tiltakstype = TiltakstypeFixtures.Gruppe

        beforeEach {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype)

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.insert(ArenaEntityMapping(ArenaTable.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id))
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

            events.forEach {
                consumer.processEvent(it).status shouldBe ArenaEvent.ProcessingStatus.Ignored
            }
            database.assertThat("avtale").isEmpty
        }

        test("ignore avtaler ended before 2023") {
            val consumer = createConsumer(database.db)

            val event = createArenaAvtaleInfoEvent(Insert) {
                it.copy(DATO_TIL = "2022-12-31 00:00:00")
            }
            consumer.processEvent(event).status shouldBe ArenaEvent.ProcessingStatus.Ignored
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

            val e1 = createArenaAvtaleInfoEvent(Insert)
            consumer.processEvent(e1).status shouldBe ArenaEvent.ProcessingStatus.Processed
            database.assertThat("avtale").row().value("status").isEqualTo(Avtale.Status.Aktiv.name)

            val e2 = createArenaAvtaleInfoEvent(Update) {
                it.copy(AVTALESTATUSKODE = Avtalestatuskode.Planlagt)
            }
            consumer.processEvent(e2).status shouldBe ArenaEvent.ProcessingStatus.Processed
            database.assertThat("avtale").row().value("status").isEqualTo(Avtale.Status.Planlagt.name)

            val e3 = createArenaAvtaleInfoEvent(Update) {
                it.copy(AVTALESTATUSKODE = Avtalestatuskode.Avsluttet)
            }
            consumer.processEvent(e3).status shouldBe ArenaEvent.ProcessingStatus.Processed
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
                val event = consumer.processEvent(createArenaAvtaleInfoEvent(Insert))

                event.status shouldBe ArenaEvent.ProcessingStatus.Failed
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.NotFound)
                    }
                )

                val consumer = createConsumer(database.db, engine)
                val event = consumer.processEvent(createArenaAvtaleInfoEvent(Insert))

                event.status shouldBe ArenaEvent.ProcessingStatus.Invalid
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
                val event = consumer.processEvent(createArenaAvtaleInfoEvent(Insert))

                event.status shouldBe ArenaEvent.ProcessingStatus.Failed
            }

            test("should call api with mapped event payload when all services responds with success") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(ArenaOrdsArrangor("123456", "1000000"))
                    },
                    "/api/v1/internal/arena/avtale.*" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                val event = createArenaAvtaleInfoEvent(Insert)
                consumer.processEvent(event)

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

                consumer.processEvent(createArenaAvtaleInfoEvent(Delete))

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

    return AvtaleInfoEventProcessor(
        ConsumerConfig("avtaleinfoendret", "avtaleinfoendret"),
        ArenaEventRepository(db),
        entities,
        client,
        ords,
    )
}
