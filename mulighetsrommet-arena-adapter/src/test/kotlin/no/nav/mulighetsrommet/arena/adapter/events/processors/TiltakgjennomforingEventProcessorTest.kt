package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.ArenaGjennomforingDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.arena.UpsertTiltaksgjennomforingResponse
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.clients.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.databaseConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakgjennomforingEvent
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Delete
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Insert
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.Update
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEntityMappingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.SakRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.TiltakshistorikkArenaGjennomforing
import no.nav.tiltak.historikk.TiltakshistorikkClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID

class TiltakgjennomforingEventProcessorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    afterEach {
        database.truncateAll()
    }

    val tiltakshistorikkStartDate = LocalDateTime.now().minus(Period.ofYears(5))

    val dateBeforeTiltakshistorikkStartDate = tiltakshistorikkStartDate.minusDays(1)

    context("handleEvent") {
        val entities = ArenaEntityService(
            mappings = ArenaEntityMappingRepository(database.db),
            tiltakstyper = TiltakstypeRepository(database.db),
            saker = SakRepository(database.db),
            tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db),
        )

        fun createProcessor(
            engine: HttpClientEngine = createMockEngine(),
        ): TiltakgjennomforingEventProcessor {
            val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
                "Bearer token"
            }

            val client = MulighetsrommetApiClient(engine, baseUri = "http://mr-api") {
                "Bearer token"
            }

            val tiltakshistorikkClient = TiltakshistorikkClient(engine, baseUrl = "http://tiltakshistorikk") {
                "Bearer token"
            }

            return TiltakgjennomforingEventProcessor(
                config = TiltakgjennomforingEventProcessor.Config(),
                entities = entities,
                ords = ords,
                mulighetsrommetApiClient = client,
                tiltakshistorikkClient = tiltakshistorikkClient,
            )
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
            test("should save the event with status Failed when dependent sak is missing") {
                val tiltakstyper = TiltakstypeRepository(database.db)
                tiltakstyper.upsert(TiltakstypeFixtures.Gruppe)

                val processor = createProcessor()

                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))
                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "insert or update on table \"tiltaksgjennomforing\" violates foreign key constraint \"tiltaksgjennomforing_sak_id_fkey\""
                }
                database.assertTable("tiltaksgjennomforing").isEmpty
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
                database.assertTable("tiltaksgjennomforing").isEmpty
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

            database.assertTable("tiltaksgjennomforing").isEmpty
        }

        context("når tiltaksgjennomføringen er et individuelt tiltak") {
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

            test("skal sende gjennomføring til tiltakshistorikk når tiltaket ikke administreres i Tiltaksadministrasjon") {
                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(
                            ArenaOrdsArrangor("123456789", "000000000"),
                        )
                    }
                    put("http://tiltakshistorikk/api/v1/intern/arena/gjennomforing") {
                        respondOk()
                    }
                }

                val processor = createProcessor(engine)

                val eventWithOldSluttDato = createArenaTiltakgjennomforingEvent(
                    Insert,
                    TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell,
                ) {
                    it.copy(DATO_TIL = dateBeforeTiltakshistorikkStartDate.format(ArenaTimestampFormatter))
                }

                val (event, mapping) = prepareEvent(eventWithOldSluttDato)
                processor.handleEvent(event).shouldBeRight().should { it.status shouldBe Handled }

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<TiltakshistorikkArenaGjennomforing>().apply {
                        id shouldBe mapping.entityId
                        arrangorOrganisasjonsnummer shouldBe Organisasjonsnummer("123456789")
                        deltidsprosent shouldBe 100.0
                    }
                }
            }
        }

        context("når tiltaksgjennomføringen er et gruppetiltak") {
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

                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    }
                    put("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing") {
                        respondJson(UpsertTiltaksgjennomforingResponse(sanityId = null))
                    }
                    delete("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}") { respondOk() }
                }
                val processor = createProcessor(engine)

                processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
                database.assertTable("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("navn").isEqualTo("Navn 1")

                val e2 = createArenaTiltakgjennomforingEvent(Update) {
                    it.copy(
                        REG_DATO = tiltakshistorikkStartDate.format(ArenaTimestampFormatter),
                        LOKALTNAVN = "Navn 2",
                    )
                }
                processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
                database.assertTable("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("navn").isEqualTo("Navn 2")

                val e3 = createArenaTiltakgjennomforingEvent(Delete) {
                    it.copy(LOKALTNAVN = "Navn 1")
                }
                processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
                database.assertTable("tiltaksgjennomforing").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("navn").isEqualTo("Navn 1")
            }

            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondError(HttpStatusCode.InternalServerError)
                    }
                }
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Internal Server Error"
                }
            }

            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondError(HttpStatusCode.NotFound)
                    }
                }
                val processor = createProcessor(engine)
                val (event) = prepareEvent(createArenaTiltakgjennomforingEvent(Insert))

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Fant ikke arrangør i Arena ORDS"
                }
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    }
                    put("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing") {
                        respondError(HttpStatusCode.InternalServerError)
                    }
                }
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

                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    }
                    put("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing") {
                        respondJson(UpsertTiltaksgjennomforingResponse(null))
                    }
                    delete("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}") { respondOk() }
                }
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaGjennomforingDbo>().apply {
                        id shouldBe mapping.entityId
                        arenaKode shouldBe tiltakstype.tiltakskode
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

                        val engine = createMockEngine {
                            get("/ords/arbeidsgiver") {
                                respondJson(ArenaOrdsArrangor("123456", "000000"))
                            }
                            put("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing") {
                                respondJson(UpsertTiltaksgjennomforingResponse(null))
                            }
                            delete("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}") { respondOk() }
                        }
                        val processor = createProcessor(engine)

                        processor.handleEvent(event).shouldBeRight()

                        engine.requestHistory.last().apply {
                            method shouldBe HttpMethod.Put

                            decodeRequestBody<ArenaGjennomforingDbo>().apply {
                                id shouldBe mapping.entityId
                                avslutningsstatus shouldBe expectedStatus
                            }
                        }
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
                val engine = createMockEngine {
                    get("/ords/arbeidsgiver") {
                        respondJson(ArenaOrdsArrangor("123456", "000000"))
                    }
                    put("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing") {
                        respondJson(UpsertTiltaksgjennomforingResponse(sanityId))
                    }
                    delete("http://mr-api/api/v1/intern/arena/tiltaksgjennomforing/${mapping.entityId}") { respondOk() }
                }
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()
                entities.getTiltaksgjennomforingOrNull(mapping.entityId) shouldNotBe null
                entities.getTiltaksgjennomforingOrNull(mapping.entityId)?.sanityId shouldBe sanityId
            }
        }
    }
})
