package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakdeltakerEvent
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Ignored
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Failed
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.Invalid
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.ArenaTimestampFormatter
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.LocalDateTime
import java.util.*

class TiltakdeltakerEventProcessorTest : FunSpec({
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

        fun createProcessor(engine: HttpClientEngine = createMockEngine()): TiltakdeltakerEventProcessor {
            val client = MulighetsrommetApiClient(engine, baseUri = "api") {
                "Bearer token"
            }

            val ords = ArenaOrdsProxyClientImpl(engine, baseUrl = "") {
                "Bearer token"
            }

            return TiltakdeltakerEventProcessor(entities, client, ords)
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
                database.assertThat("deltaker").isEmpty
            }
        }

        context("when dependent events has been processed") {
            val tiltakstypeGruppe = TiltakstypeFixtures.Gruppe
            val sak = Sak(sakId = 1, lopenummer = 1, aar = 2022, enhet = "2990")
            val tiltaksgjennomforing = Tiltaksgjennomforing(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = 3,
                sakId = 1,
                tiltakskode = "INDOPPFAG",
                arrangorId = 123,
                navn = "Gjennomføring gruppe",
                status = "GJENNOMFOR",
                fraDato = LocalDateTime.of(2023, 1, 1, 0, 0),
                tilDato = null,
                apentForInnsok = true,
                antallPlasser = null,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            val tiltakstypeIndividuell = TiltakstypeFixtures.Individuell
            val sakIndividuell = Sak(sakId = 2, lopenummer = 2, aar = 2022, enhet = "2990")
            val tiltaksgjennomforingIndividuell = Tiltaksgjennomforing(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = 4,
                sakId = 2,
                tiltakskode = "AMO",
                arrangorId = 123,
                navn = "Gjennomføring individuell",
                status = "GJENNOMFOR",
                fraDato = LocalDateTime.of(2023, 1, 1, 0, 0),
                tilDato = null,
                apentForInnsok = true,
                antallPlasser = null,
                avtaleId = null,
                deltidsprosent = 100.0,
            )

            /**
             * Tiltakskode er ikke en del av av [Tiltakskoder.AmtTiltak] og opphav på deltakelser for denne tiltakstypen
             * er dermed Arena (og ikke Komet)
             */
            val tiltakstypeGruppeOpphavArena = TiltakstypeFixtures.Gruppe.copy(
                id = UUID.randomUUID(),
                tiltakskode = "IPSUNG",
            )
            val sakOpphavArena = Sak(sakId = 3, lopenummer = 3, aar = 2022, enhet = "2990")
            val tiltaksgjennomforingOpphavArena = tiltaksgjennomforing.copy(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = 5,
                sakId = 3,
                tiltakskode = tiltakstypeGruppeOpphavArena.tiltakskode,
                navn = "Gjennomføring opphav Arena",
            )

            beforeEach {
                val mappings = ArenaEntityMappingRepository(database.db)

                val tiltakstyper = TiltakstypeRepository(database.db)
                listOf(tiltakstypeGruppe, tiltakstypeIndividuell, tiltakstypeGruppeOpphavArena).forEach {
                    tiltakstyper.upsert(it).getOrThrow()
                    mappings.upsert(
                        ArenaEntityMapping(ArenaTable.Tiltakstype, it.tiltakskode, it.id, Handled),
                    )
                }

                val saker = SakRepository(database.db)
                listOf(sak, sakIndividuell, sakOpphavArena).forEach {
                    saker.upsert(it).getOrThrow()
                }

                val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
                listOf(tiltaksgjennomforing, tiltaksgjennomforingIndividuell, tiltaksgjennomforingOpphavArena).forEach {
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

            test("should be ignored when it's no longer relevant for brukers tiltakshistorikk") {
                val datoBeforeTiltakshistorikkStart = LocalDateTime.now()
                    .minus(Tiltakshistorikk.TiltakshistorikkTimePeriod)
                    .minusDays(1)
                    .format(ArenaTimestampFormatter)

                val processor = createProcessor()

                val eventWithOldSluttDato = createArenaTiltakdeltakerEvent(Insert) {
                    it.copy(DATO_TIL = datoBeforeTiltakshistorikkStart)
                }
                val eventWithOldRegDato = createArenaTiltakdeltakerEvent(Insert) {
                    it.copy(REG_DATO = datoBeforeTiltakshistorikkStart)
                }
                forAll(row(eventWithOldSluttDato), row(eventWithOldRegDato)) { event ->
                    runBlocking {
                        processor.handleEvent(event) shouldBeRight ProcessingResult(
                            Ignored,
                            "Deltaker ignorert fordi den ikke lengre er relevant for brukers tiltakshistorikk",
                        )
                    }
                }
            }

            test("should be ignored when dependent tiltaksgjennomforing is ignored") {
                val processor = createProcessor()

                entities.upsertMapping(
                    ArenaEntityMapping(
                        ArenaTable.Tiltaksgjennomforing,
                        tiltaksgjennomforing.tiltaksgjennomforingId.toString(),
                        UUID.randomUUID(),
                        Ignored,
                    ),
                )
                val event = createArenaTiltakdeltakerEvent(Insert)

                processor.handleEvent(event).shouldBeRight().should { it.status shouldBe Ignored }
            }

            test("should treat all operations as upserts") {
                val (e1, mapping) = prepareEvent(
                    createArenaTiltakdeltakerEvent(Insert) { it.copy(DELTAKERSTATUSKODE = "GJENN") },
                    Ignored,
                )

                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk" to { respondOk() },
                    "/api/v1/internal/arena/tiltakshistorikk/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(e1).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("deltaker").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo("DELTAR")

                val e2 = createArenaTiltakdeltakerEvent(Update) { it.copy(DELTAKERSTATUSKODE = "FULLF") }
                processor.handleEvent(e2).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("deltaker").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo("AVSLUTTET")

                val e3 = createArenaTiltakdeltakerEvent(Delete) { it.copy(DELTAKERSTATUSKODE = "FULLF") }
                processor.handleEvent(e3).shouldBeRight().should { it.status shouldBe Handled }
                database.assertThat("deltaker").row()
                    .value("id").isEqualTo(mapping.entityId)
                    .value("status").isEqualTo("AVSLUTTET")
            }

            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondError(HttpStatusCode.InternalServerError) },
                    "/api/v1/internal/arena/deltaker" to { respondOk() },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaTiltakdeltakerEvent(Insert), Ignored)

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Internal Server Error"
                }
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondError(HttpStatusCode.NotFound) },
                    "/api/v1/internal/arena/tiltakshistorikk" to { respondOk() },
                )

                val processor = createProcessor(engine)
                val (event) = prepareEvent(createArenaTiltakdeltakerEvent(Insert), Ignored)
                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Invalid
                    it.message shouldContain "Fant ikke norsk ident i Arena ORDS"
                }
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk" to {
                        respondError(
                            HttpStatusCode.InternalServerError,
                        )
                    },
                )
                val processor = createProcessor(engine)

                val (event) = prepareEvent(createArenaTiltakdeltakerEvent(Insert), Ignored)

                processor.handleEvent(event).shouldBeLeft().should {
                    it.status shouldBe Failed
                    it.message shouldContain "Internal Server Error"
                }
            }

            test("should call api with tiltakshistorikk when all services responds with success") {
                val (event, mapping) = prepareEvent(createArenaTiltakdeltakerEvent(Insert), Ignored)

                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk" to { respondOk() },
                    "/api/v1/internal/arena/tiltakshistorikk/${mapping.entityId}" to { respondOk() },
                )
                val processor = createProcessor(engine)

                processor.handleEvent(event).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaTiltakshistorikkDbo>().apply {
                        shouldBeInstanceOf<ArenaTiltakshistorikkDbo.Gruppetiltak>()
                        id shouldBe mapping.entityId
                        tiltaksgjennomforingId shouldBe tiltaksgjennomforing.id
                        norskIdent shouldBe "12345678910"
                    }
                }

                processor.handleEvent(createArenaTiltakdeltakerEvent(Delete)).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Delete

                    url.getLastPathParameterAsUUID() shouldBe mapping.entityId
                }
            }

            test("should include arbeidsgiver from ORDS when deltakelse is individuelt tiltak") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk" to { respondOk() },
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor("123456", "000000"),
                        )
                    },
                )
                val processor = createProcessor(engine)

                val (event, mapping) = prepareEvent(
                    createArenaTiltakdeltakerEvent(Insert) {
                        it.copy(
                            TILTAKDELTAKER_ID = 2,
                            TILTAKGJENNOMFORING_ID = tiltaksgjennomforingIndividuell.tiltaksgjennomforingId,
                        )
                    },
                    Ignored,
                )

                processor.handleEvent(event).shouldBeRight()

                engine.requestHistory.last().apply {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<ArenaTiltakshistorikkDbo>().apply {
                        shouldBeInstanceOf<ArenaTiltakshistorikkDbo.IndividueltTiltak>()
                        id shouldBe mapping.entityId
                        beskrivelse shouldBe tiltaksgjennomforingIndividuell.navn
                        arrangorOrganisasjonsnummer shouldBe "123456"
                        tiltakstypeId shouldBe tiltakstypeIndividuell.id
                        norskIdent shouldBe "12345678910"
                    }
                }
            }

            test("should call api with deltaker when deltakelse has opphav=Arena") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/tiltakshistorikk" to { respondOk() },
                    "/api/v1/internal/arena/deltaker" to { respondOk() },
                )
                val processor = createProcessor(engine)

                val (event, mapping) = prepareEvent(
                    createArenaTiltakdeltakerEvent(Insert) {
                        it.copy(TILTAKGJENNOMFORING_ID = tiltaksgjennomforingOpphavArena.tiltaksgjennomforingId)
                    },
                    Ignored,
                )

                processor.handleEvent(event).shouldBeRight()

                engine.requestHistory shouldHaveSingleElement {
                    it.method == HttpMethod.Put &&
                        it.url.encodedPath == "/api/v1/internal/arena/tiltakshistorikk" &&
                        it.decodeRequestBody<ArenaTiltakshistorikkDbo>().id == mapping.entityId
                }

                engine.requestHistory shouldHaveSingleElement {
                    it.method == HttpMethod.Put &&
                        it.url.encodedPath == "/api/v1/internal/arena/deltaker" &&
                        it.decodeRequestBody<DeltakerDbo>().id == mapping.entityId
                }
            }
        }
    }
})
