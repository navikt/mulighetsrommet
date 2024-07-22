package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.flatMap
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaAvtaleInfoEvent
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakgjennomforingEvent
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaAvtaleInfo
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.UpsertTiltaksgjennomforingResponse
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakgjennomforingEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterEach {
        database.db.truncateAll()
    }

    val tiltakshistorikkStartDate = LocalDateTime.now().minus(Tiltakshistorikk.TiltakshistorikkTimePeriod)

    val dateBeforeTiltakshistorikkStartDate = tiltakshistorikkStartDate.minusDays(1)

    context("handleEvent") {
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
            deltakere = DeltakerRepository(database.db),
            avtaler = AvtaleRepository(database.db),
        )

        fun createProcessor(engine: HttpClientEngine = createMockEngine()): TiltakgjennomforingEventProcessor {
            val client = MulighetsrommetApiClient(engine, baseUri = "") {
                "Bearer token"
            }

            val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
                "Bearer token"
            }

            return TiltakgjennomforingEventProcessor(entities, client, ords)
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

        fun upsertAvtale(event: ArenaEvent, mapping: ArenaEntityMapping) {
            event.decodePayload<ArenaAvtaleInfo>()
                .toAvtale(mapping.entityId)
                .flatMap { entities.upsertAvtale(it) }
                .shouldBeRight()
        }

        context("when dependent events has not been processed") {
            test("should save the event with status Failed when dependent sak is missing") {
                val tiltakstyper = TiltakstypeRepository(database.db)
                tiltakstyper.upsert(TiltakstypeFixtures.Gruppe)

                val processor = createProcessor()

                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))
                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "insert or update on table \"tiltaksgjennomforing\" violates foreign key constraint \"tiltaksgjennomforing_sak_id_fkey\""
                }
                database.assertThat("tiltaksgjennomforing").isEmpty
            }

            test("should save the event with status Failed when dependent tiltakstype is missing") {
                val saker = SakRepository(database.db)
                saker.upsert(
                    Sak(
                        sakId = 13572352,
                        lopenummer = 123,
                        aar = 2022,
                        enhet = "2990",
                    ),
                )

                val processor = createProcessor()
                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "insert or update on table \"tiltaksgjennomforing\" violates foreign key constraint \"tiltaksgjennomforing_tiltakskode_fkey\""
                }
                database.assertThat("tiltaksgjennomforing").isEmpty
            }
        }

        test("should ignore gjennomføringer when required fields are missing") {
            val processor = createProcessor()

            listOf(
                createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(DATO_FRA = null)
                },
                createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(LOKALTNAVN = null)
                },
                createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(ARBGIV_ID_ARRANGOR = null)
                },
            ).forEach { event ->
                processor.handleEvent(event).shouldBeRight().should {
                    it.status shouldBe Ignored
                }
            }

            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        context("when tiltaksgjennomføring is individuell") {
            val tiltakstype = TiltakstypeFixtures.Individuell

            beforeEach {
                val saker = SakRepository(database.db)
                saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022, enhet = "2990"))

                val tiltakstyper = TiltakstypeRepository(database.db)
                tiltakstyper.upsert(tiltakstype)
                entities.upsertMapping(
                    ArenaEntityMapping(ArenaTable.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id, Handled),
                )
            }

            context("when tiltaksgjennomføring is individuelt tiltak") {
                test("should upsert gjennomføringer") {
                    val processor = createProcessor()

                    val eventWithOldSluttDato = createArenaTiltakgjennomforingEvent(
                        Insert,
                        TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell,
                    ) {
                        it.copy(DATO_TIL = dateBeforeTiltakshistorikkStartDate.format(ArenaTimestampFormatter))
                    }
                    val eventCreatedAfterAktivitetsplanen = createArenaTiltakgjennomforingEvent(
                        Insert,
                        TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell,
                    ) {
                        it.copy(REG_DATO = tiltakshistorikkStartDate.format(ArenaTimestampFormatter))
                    }

                    forAll(row(eventWithOldSluttDato), row(eventCreatedAfterAktivitetsplanen)) { event ->
                        runBlocking {
                            val (e) = prepareEvent(event)
                            processor.handleEvent(e).shouldBeRight().should { it.status shouldBe Handled }
                        }
                    }
                }

                test("should not send gjennomføringer to mr-api") {
                    val engine = MockEngine { respondOk() }
                    val processor = createProcessor(engine)

                    val (event) = prepareEvent(
                        createArenaTiltakgjennomforingEvent(
                            Insert,
                            TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell,
                        ),
                    )

                    processor.handleEvent(event).shouldBeRight()
                    engine.requestHistory.shouldBeEmpty()
                }
            }
        }

        context("when tiltaksgjennomføring is gruppetiltak") {
            val tiltakstype = TiltakstypeFixtures.Gruppe

            beforeEach {
                val saker = SakRepository(database.db)
                saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022, enhet = "2990"))

                val tiltakstyper = TiltakstypeRepository(database.db)
                tiltakstyper.upsert(tiltakstype)
                entities.upsertMapping(
                    ArenaEntityMapping(ArenaTable.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id, Handled),
                )
            }

            test("should treat all operations on gruppetiltak as upserts") {
                val (e1, mapping) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) {
                        it.copy(
                            REG_DATO = dateBeforeTiltakshistorikkStartDate.format(ArenaTimestampFormatter),
                            LOKALTNAVN = "Navn 1",
                        )
                    },
                )

                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing" to {
                        respondJson(UpsertTiltaksgjennomforingResponse(sanityId = null))
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("navn").isEqualTo("Navn 1")

                val e2 = createArenaTiltakgjennomforingEvent(Update) {
                    it.copy(
                        REG_DATO = tiltakshistorikkStartDate.format(ArenaTimestampFormatter),
                        LOKALTNAVN = "Navn 2",
                    )
                }
                processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("navn").isEqualTo("Navn 2")

                val e3 = createArenaTiltakgjennomforingEvent(Delete) {
                    it.copy(LOKALTNAVN = "Navn 1")
                }
                processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("navn").isEqualTo("Navn 1")
            }

            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.InternalServerError)
                    },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Internal Server Error"
                }
            }

            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(HttpStatusCode.NotFound)
                    },
                )
                val processor = createProcessor(engine)
                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Fant ikke arrangør i Arena ORDS"
                }
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing" to {
                        respondError(HttpStatusCode.InternalServerError)
                    },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Internal Server Error"
                }
            }

            test("should call api with mapped event payload when all services responds with success") {
                val (event, mapping) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) {
                        it.copy(
                            DATO_FRA = "2022-11-11 00:00:00",
                            DATO_TIL = "2023-11-11 00:00:00",
                            PROSENT_DELTID = 55.33,
                        )
                    },
                )

                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing" to {
                        respondJson(UpsertTiltaksgjennomforingResponse(null))
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaTiltaksgjennomforingDbo>().apply {
                        id shouldBe mapping.entityId
                        tiltakstypeId shouldBe tiltakstype.id
                        tiltaksnummer shouldBe "2022#123"
                        arrangorOrganisasjonsnummer shouldBe "123456"
                        startDato shouldBe LocalDate.of(2022, 11, 11)
                        sluttDato shouldBe LocalDate.of(2023, 11, 11)
                        avslutningsstatus shouldBe Avslutningsstatus.IKKE_AVSLUTTET
                        deltidsprosent shouldBe 55.33
                    }
                }

                processor.handleEvent(createArenaTiltakgjennomforingEvent(Delete)).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Delete

                    url.getLastPathParameterAsUUID() shouldBe mapping.entityId
                }
            }

            test("should map status to avslutningsstatus") {
                val gjennomfores = createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(TILTAKSTATUSKODE = "GJENNOMFOR")
                }
                val avlyst = createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(TILTAKSTATUSKODE = "AVLYST")
                }
                val avbrutt = createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(TILTAKSTATUSKODE = "AVBRUTT")
                }
                val avsluttetBeforeSluttdato = createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(
                        TILTAKSTATUSKODE = "AVSLUTT",
                        DATO_FRA = "2022-11-11 00:00:00",
                        DATO_TIL = LocalDateTime.now().plusYears(1).format(ArenaTimestampFormatter),
                    )
                }
                val avsluttetAfterSluttdato = createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(
                        TILTAKSTATUSKODE = "AVSLUTT",
                        DATO_FRA = "2022-11-11 00:00:00",
                        DATO_TIL = "2023-11-11 00:00:00",
                    )
                }
                forAll(
                    row(gjennomfores, Avslutningsstatus.IKKE_AVSLUTTET),
                    row(avlyst, Avslutningsstatus.AVLYST),
                    row(avbrutt, Avslutningsstatus.AVBRUTT),
                    row(avsluttetBeforeSluttdato, Avslutningsstatus.AVBRUTT),
                    row(avsluttetAfterSluttdato, Avslutningsstatus.AVSLUTTET),
                ) { event2, expectedStatus ->
                    runBlocking {
                        val (event, mapping) = prepareEvent(event2)

                        val engine = createMockEngine(
                            "/ords/arbeidsgiver" to {
                                respondJson(
                                    ArenaOrdsArrangor("123456", "000000"),
                                )
                            },
                            "/api/v1/intern/arena/tiltaksgjennomforing" to {
                                respondJson(UpsertTiltaksgjennomforingResponse(null))
                            },
                            "/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}" to { respondOk() },
                        )
                        val processor = createProcessor(engine)

                        processor.handleEvent(event).shouldBeRight()

                        engine.requestHistory.last().apply {
                            method shouldBe HttpMethod.Put

                            decodeRequestBody<ArenaTiltaksgjennomforingDbo>().apply {
                                id shouldBe mapping.entityId
                                avslutningsstatus shouldBe expectedStatus
                            }
                        }
                    }
                }
            }

            test("should fail when dependent avtale is missing") {
                val processor = createProcessor()

                val (event) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) { it.copy(AVTALE_ID = 1) },
                )

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "ArenaEntityMapping mangler for arenaTable=AvtaleInfo og arenaId=1"
                }
            }

            test("should fail when dependent avtale is Unhandled") {
                val processor = createProcessor()

                prepareEvent(
                    createArenaAvtaleInfoEvent(Insert) { it.copy(AVTALE_ID = 1) },
                    Unhandled,
                )

                val (event) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) { it.copy(AVTALE_ID = 1) },
                )

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Avtale har enda ikke blitt prosessert"
                }
            }

            test("should not keep reference to avtale when avtale is Ignored") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(ArenaOrdsArrangor("123456", "000000"))
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing" to {
                        respondJson(UpsertTiltaksgjennomforingResponse(null))
                    },
                )
                val processor = createProcessor(engine)

                val (avtaleEvent, avtaleMapping) = prepareEvent(
                    createArenaAvtaleInfoEvent(Insert) { it.copy(AVTALE_ID = 1) },
                    Ignored,
                )
                upsertAvtale(avtaleEvent, avtaleMapping)

                val (event, mapping) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) { it.copy(AVTALE_ID = 1) },
                )
                processor.handleEvent(event).shouldBeRight()

                database.assertThat("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("avtale_id").isNull
            }

            test("should keep reference to avtale when avtale is Handled") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing" to {
                        respondJson(UpsertTiltaksgjennomforingResponse(null))
                    },
                )
                val processor = createProcessor(engine)

                val (avtaleEvent, avtaleMapping) = prepareEvent(
                    createArenaAvtaleInfoEvent(Insert) { it.copy(AVTALE_ID = 1) },
                    Handled,
                )
                upsertAvtale(avtaleEvent, avtaleMapping)

                val (event, mapping) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) { it.copy(AVTALE_ID = 1) },
                )
                processor.handleEvent(event).shouldBeRight()

                database.assertThat("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("avtale_id").isEqualTo(1)

                engine.requestHistory.last().apply {
                    decodeRequestBody<ArenaTiltaksgjennomforingDbo>().apply {
                        id shouldBe mapping.entityId
                        avtaleId shouldBe avtaleMapping.entityId
                    }
                }
            }

            test("should save sanityId when it is returned from api") {
                val (event, mapping) = prepareEvent(
                    createArenaTiltakgjennomforingEvent(Insert) {
                        it.copy(
                            DATO_FRA = "2022-11-11 00:00:00",
                            DATO_TIL = "2023-11-11 00:00:00",
                        )
                    },
                )

                val sanityId = UUID.randomUUID()
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing" to {
                        respondJson(UpsertTiltaksgjennomforingResponse(sanityId))
                    },
                    "/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()
                entities.getTiltaksgjennomforingOrNull(mapping.entityId) shouldNotBe null
                entities.getTiltaksgjennomforingOrNull(mapping.entityId)?.sanityId shouldBe sanityId
            }
        }
    }
})
