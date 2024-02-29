package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaAvtaleInfoEvent
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.Avtalestatuskode
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Invalid
import no.nav.mulighetsrommet.arena.adapter.models.db.Avtale
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson

class AvtaleInfoEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    context("handleEvent") {
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            avtaler = AvtaleRepository(database.db),
        )

        fun createProcessor(engine: HttpClientEngine = createMockEngine()): AvtaleInfoEventProcessor {
            val client = MulighetsrommetApiClient(engine, baseUri = "api") {
                "Bearer token"
            }

            val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
                "Bearer token"
            }

            return AvtaleInfoEventProcessor(entities, client, ords)
        }

        fun prepareEvent(event: ArenaEvent): Pair<ArenaEvent, ArenaEntityMapping> {
            val mapping = entities.getOrCreateMapping(event)
            return Pair(event, mapping)
        }

        context("when dependent events has not been processed") {
            test("should save the event with status Failed when dependent tiltakstype is missing") {
                val processor = createProcessor()

                val (event) = prepareEvent(createArenaAvtaleInfoEvent(Insert))
                val result = processor.handleEvent(event)

                result.shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Key (tiltakskode)=(INDOPPFAG) is not present in table \"tiltakstype\""
                }
                database.assertThat("avtale").isEmpty
            }
        }

        context("when dependent tiltakstype has been processed") {
            val tiltakstype = TiltakstypeFixtures.Gruppe

            beforeEach {
                val tiltakstyper = TiltakstypeRepository(database.db)
                tiltakstyper.upsert(tiltakstype)

                val mappings = ArenaEntityMappingRepository(database.db)
                mappings.upsert(
                    ArenaEntityMapping(
                        ArenaTable.Tiltakstype,
                        tiltakstype.tiltakskode,
                        tiltakstype.id,
                        Handled,
                    ),
                )
            }

            test("ignore avtaler when required fields are missing") {
                val processor = createProcessor()

                val events = listOf(
                    createArenaAvtaleInfoEvent(Insert) {
                        it.copy(AVTALENAVN = null)
                    },
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
                    processor.handleEvent(event).shouldBeRight().should { it.status shouldBe Ignored }
                }
                database.assertThat("avtale").isEmpty
            }

            test("ignore avtaler ended before 2023") {
                val processor = createProcessor()

                val event = createArenaAvtaleInfoEvent(Insert) {
                    it.copy(DATO_TIL = "2022-12-31 00:00:00")
                }

                processor.handleEvent(event).shouldBeRight().should { it.status shouldBe Ignored }
                database.assertThat("avtale").isEmpty
            }

            test("should treat all operations as upserts") {
                val (e1, mapping) = prepareEvent(createArenaAvtaleInfoEvent(Insert))

                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(ArenaOrdsArrangor("123456", "1000000"))
                    },
                    "/api/v1/internal/arena/avtale" to { respondOk() },
                    "/api/v1/internal/arena/avtale/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("avtale").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo(Avtale.Status.Aktiv.name)

                val e2 = createArenaAvtaleInfoEvent(Update) {
                    it.copy(AVTALESTATUSKODE = Avtalestatuskode.Planlagt)
                }
                processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("avtale").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo(Avtale.Status.Planlagt.name)

                val e3 = createArenaAvtaleInfoEvent(Delete) {
                    it.copy(AVTALESTATUSKODE = Avtalestatuskode.Avsluttet)
                }
                processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("avtale").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo(Avtale.Status.Avsluttet.name)
            }

            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.InternalServerError)
                    },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaAvtaleInfoEvent(Insert))
                val result = processor.handleEvent(event)

                result.shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Unexpected response from arena-ords-proxy"
                }
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.NotFound)
                    },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaAvtaleInfoEvent(Insert))
                val result = processor.handleEvent(event)

                result.shouldBeLeft().should {
                    it.status shouldBe Invalid
                    it.message shouldContain "Fant ikke leverandør i Arena ORDS"
                }
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "100000"),
                        )
                    },
                    "/api/v1/internal/arena/avtale" to {
                        respondError(HttpStatusCode.InternalServerError)
                    },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaAvtaleInfoEvent(Insert))
                val result = processor.handleEvent(event)

                result.shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Internal Server Error"
                }
            }

            test("should call api with mapped event payload when all services responds with success") {
                val (event, mapping) = prepareEvent(createArenaAvtaleInfoEvent(Insert))

                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(ArenaOrdsArrangor("123456", "1000000"))
                    },
                    "/api/v1/internal/arena/avtale" to { respondOk() },
                    "/api/v1/internal/arena/avtale/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaAvtaleDbo>().apply {
                        id shouldBe mapping.entityId
                        tiltakstypeId shouldBe tiltakstype.id
                        avtalenummer shouldBe "2022#2000"
                        leverandorOrganisasjonsnummer shouldBe "1000000"
                        avtaletype shouldBe Avtaletype.Rammeavtale
                        avslutningsstatus shouldBe Avslutningsstatus.IKKE_AVSLUTTET
                    }
                }

                processor.handleEvent(createArenaAvtaleInfoEvent(Delete)).shouldBeRight()

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    url.getLastPathParameterAsUUID() shouldBe mapping.entityId
                }
            }

            test("should not call api with handle event when status is OVERF from Arena") {
                val (event, mapping) = prepareEvent(
                    createArenaAvtaleInfoEvent(Insert) {
                        it.copy(
                            AVTALESTATUSKODE = Avtalestatuskode.Overfort,
                        )
                    },
                )

                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(ArenaOrdsArrangor("123456", "1000000"))
                    },
                    "/api/v1/internal/arena/avtale" to { respondOk() },
                    "/api/v1/internal/arena/avtale/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()

                database.assertThat("avtale").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo(Avtale.Status.Overfort.name)

                engine.requestHistory.shouldBeEmpty()
            }
        }
    }
})
