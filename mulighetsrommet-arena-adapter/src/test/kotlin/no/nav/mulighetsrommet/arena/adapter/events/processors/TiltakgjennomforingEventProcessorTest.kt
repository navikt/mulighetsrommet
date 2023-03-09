package no.nav.mulighetsrommet.arena.adapter.events.processors

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.createDatabaseTestConfig
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.arena.adapter.fixtures.createArenaTiltakgjennomforingEvent
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ProcessingStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.getLastPathParameterAsUUID
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.LocalDate

class TiltakgjennomforingEventProcessorTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    val regDatoBeforeAktivitetsplanen = AktivitetsplanenLaunchDate
        .minusDays(1)
        .format(ArenaUtils.TimestampFormatter)

    val regDatoAfterAktivitetsplanen = AktivitetsplanenLaunchDate
        .format(ArenaUtils.TimestampFormatter)

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when dependent tiltakstype is missing") {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(TiltakstypeFixtures.Gruppe)

            val consumer = createConsumer(database.db, MockEngine { respondOk() })
            val event = createArenaTiltakgjennomforingEvent(Insert)

            consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Failed }
            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should save the event with status Failed when dependent sak is missing") {
            val saker = SakRepository(database.db)
            saker.upsert(
                Sak(
                    sakId = 13572352,
                    lopenummer = 123,
                    aar = 2022,
                    enhet = "2990"
                )
            )

            val consumer = createConsumer(database.db, MockEngine { respondOk() })
            val event = createArenaTiltakgjennomforingEvent(Insert)

            consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Failed }
            database.assertThat("tiltaksgjennomforing").isEmpty
        }
    }

    context("when tiltaksgjennomføring is individuell") {
        val tiltakstype = TiltakstypeFixtures.Individuell

        beforeEach {
            val saker = SakRepository(database.db)
            saker.upsert(
                Sak(
                    sakId = 13572352,
                    lopenummer = 123,
                    aar = 2022,
                    enhet = "2990"
                )
            )

            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype)

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.insert(ArenaEntityMapping(ArenaTable.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id, ArenaEntityMapping.Status.Upserted))
        }

        test("should ignore individuelle tiltaksgjennomføringer created before Aktivitetsplanen") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)

            val event = createArenaTiltakgjennomforingEvent(
                Insert,
                TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell
            ) {
                it.copy(REG_DATO = regDatoBeforeAktivitetsplanen)
            }

            consumer.handleEvent(event).shouldBeLeft().should { it should beOfType<ProcessingError.Ignored>() }
            database.assertThat("tiltaksgjennomforing").isEmpty
            engine.requestHistory.shouldBeEmpty()
        }

        test("should ignore if DATO_FRA is null") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)

            val event = createArenaTiltakgjennomforingEvent(
                Insert,
                TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell
            ) {
                it.copy(DATO_FRA = null)
            }

            consumer.handleEvent(event).shouldBeLeft().should { it should beOfType<ProcessingError.Ignored>() }
            database.assertThat("tiltaksgjennomforing").isEmpty
            engine.requestHistory.shouldBeEmpty()
        }

        test("should upsert individuelle tiltaksgjennomføringer created after Aktivitetsplanen") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)

            val event = createArenaTiltakgjennomforingEvent(
                Insert,
                TiltaksgjennomforingFixtures.ArenaTiltaksgjennomforingIndividuell
            ) {
                it.copy(REG_DATO = regDatoAfterAktivitetsplanen)
            }

            consumer.handleEvent(event) shouldBeRight Processed
            database.assertThat("tiltaksgjennomforing").row()
                .value("tiltakskode").isEqualTo("AMO")
            engine.requestHistory.shouldBeEmpty()
        }
    }

    context("when tiltaksgjennomføring is gruppetiltak") {
        val tiltakstype = TiltakstypeFixtures.Gruppe

        beforeEach {
            val saker = SakRepository(database.db)
            saker.upsert(
                Sak(
                    sakId = 13572352,
                    lopenummer = 123,
                    aar = 2022,
                    enhet = "2990"
                )
            )

            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype)

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.insert(ArenaEntityMapping(ArenaTable.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id, ArenaEntityMapping.Status.Upserted))
        }

        test("should treat all operations on gruppetiltak as upserts") {
            val engine = createMockEngine(
                "/ords/arbeidsgiver" to {
                    respondJson(ArenaOrdsArrangor("123456", "000000"))
                },
                "/api/v1/internal/arena/tiltaksgjennomforing.*" to { respondOk() }
            )

            val consumer = createConsumer(database.db, engine)

            val e1 = createArenaTiltakgjennomforingEvent(Insert) {
                it.copy(
                    REG_DATO = regDatoBeforeAktivitetsplanen,
                    LOKALTNAVN = "Navn 1"
                )
            }
            consumer.handleEvent(e1) shouldBeRight Processed
            database.assertThat("tiltaksgjennomforing").row().value("navn").isEqualTo("Navn 1")

            val e2 = createArenaTiltakgjennomforingEvent(Update) {
                it.copy(
                    REG_DATO = regDatoAfterAktivitetsplanen,
                    LOKALTNAVN = "Navn 2"
                )
            }
            consumer.handleEvent(e2) shouldBeRight Processed
            database.assertThat("tiltaksgjennomforing").row().value("navn").isEqualTo("Navn 2")

            val e3 = createArenaTiltakgjennomforingEvent(Delete) { it.copy(LOKALTNAVN = "Navn 1") }
            consumer.handleEvent(e3) shouldBeRight Processed
            database.assertThat("tiltaksgjennomforing").row().value("navn").isEqualTo("Navn 1")
        }

        context("api responses") {
            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(
                            HttpStatusCode.InternalServerError
                        )
                    }
                )

                val consumer = createConsumer(database.db, engine)
                val event = createArenaTiltakgjennomforingEvent(Insert)

                consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Failed }
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondError(
                            HttpStatusCode.NotFound
                        )
                    }
                )

                val consumer = createConsumer(database.db, engine)
                val event = createArenaTiltakgjennomforingEvent(Insert)

                consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Invalid }
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor(
                                "123456",
                                "000000"
                            )
                        )
                    },
                    "/api/v1/internal/arena/tiltaksgjennomforing" to {
                        respondError(
                            HttpStatusCode.InternalServerError
                        )
                    }
                )

                val consumer = createConsumer(database.db, engine)
                val event = createArenaTiltakgjennomforingEvent(Insert)

                consumer.handleEvent(event).shouldBeLeft().should { it.status shouldBe Failed }
            }

            test("should call api with mapped event payload when all services responds with success") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to {
                        respondJson(
                            ArenaOrdsArrangor(
                                "123456",
                                "000000"
                            )
                        )
                    },
                    "/api/v1/internal/arena/tiltaksgjennomforing.*" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                val event = createArenaTiltakgjennomforingEvent(Insert) {
                    it.copy(
                        DATO_FRA = "2022-11-11 00:00:00",
                        DATO_TIL = "2023-11-11 00:00:00"
                    )
                }
                consumer.handleEvent(event).shouldBeRight()

                val generatedId = engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    val tiltaksgjennomforing = decodeRequestBody<TiltaksgjennomforingDbo>().apply {
                        tiltakstypeId shouldBe tiltakstype.id
                        tiltaksnummer shouldBe "2022#123"
                        virksomhetsnummer shouldBe "123456"
                        startDato shouldBe LocalDate.of(2022, 11, 11)
                        sluttDato shouldBe LocalDate.of(2023, 11, 11)
                        avslutningsstatus shouldBe Avslutningsstatus.IKKE_AVSLUTTET
                    }

                    tiltaksgjennomforing.id
                }

                consumer.handleEvent(createArenaTiltakgjennomforingEvent(Delete)).shouldBeRight()

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    url.getLastPathParameterAsUUID() shouldBe generatedId
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakgjennomforingEventProcessor {
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

    return TiltakgjennomforingEventProcessor(entities, client, ords)
}
