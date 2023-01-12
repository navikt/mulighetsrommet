package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsArrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.AktivitetsplanenLaunchDate
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

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
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    tiltakskode = "INDOPPFAG",
                    rettPaaTiltakspenger = true,
                    fraDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
                    tilDato = LocalDateTime.of(2023, 1, 12, 0, 0, 0)
                )
            )

            val consumer = createConsumer(
                database.db,
                MockEngine { respondOk() }
            )

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
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

            val consumer = createConsumer(
                database.db,
                MockEngine { respondOk() }
            )

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("tiltaksgjennomforing").isEmpty
        }
    }

    context("when tiltaksgjennomføring is individuell") {
        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "AMO",
            tiltakskode = "AMO",
            rettPaaTiltakspenger = false,
            fraDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
            tilDato = LocalDateTime.of(2023, 1, 12, 0, 0, 0)
        )

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
            mappings.insert(ArenaEntityMapping(ArenaTables.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id))
        }

        test("should ignore individuelle tiltaksgjennomføringer created before Aktivitetsplanen") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)

            val event = consumer.processEvent(
                createEvent(
                    Insert,
                    tiltakskode = "AMO",
                    regDato = regDatoBeforeAktivitetsplanen
                )
            )

            event.status shouldBe Ignored
            database.assertThat("tiltaksgjennomforing").isEmpty
            engine.requestHistory.shouldBeEmpty()
        }

        test("should upsert individuelle tiltaksgjennomføringer created after Aktivitetsplanen") {
            val engine = MockEngine { respondOk() }
            val consumer = createConsumer(database.db, engine)

            val event = consumer.processEvent(
                createEvent(
                    Insert,
                    tiltakskode = "AMO",
                    regDato = regDatoAfterAktivitetsplanen
                )
            )

            event.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing").row()
                .value("tiltakskode").isEqualTo("AMO")
            engine.requestHistory.shouldBeEmpty()
        }
    }

    context("when tiltaksgjennomføring is gruppetiltak") {
        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakskode = "INDOPPFAG",
            rettPaaTiltakspenger = true,
            fraDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
            tilDato = LocalDateTime.of(2023, 1, 12, 0, 0, 0)
        )

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

            val mappings =
                ArenaEntityMappingRepository(database.db)
            mappings.insert(ArenaEntityMapping(ArenaTables.Tiltakstype, tiltakstype.tiltakskode, tiltakstype.id))
        }

        test("should treat all operations on gruppetiltak as upserts") {
            val engine = createMockEngine(
                "/ords/arbeidsgiver" to {
                    respondJson(ArenaOrdsArrangor("123456", "000000"))
                },
                "/api/v1/internal/arena/tiltaksgjennomforing" to { respondOk() }
            )

            val consumer = createConsumer(database.db, engine)

            val e1 = consumer.processEvent(
                createEvent(
                    Insert,
                    regDato = regDatoBeforeAktivitetsplanen,
                    name = "Navn 1"
                )
            )
            e1.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo("Navn 1")
                .value("status").isEqualTo("GJENNOMFOR")

            val e2 = consumer.processEvent(
                createEvent(
                    Update,
                    regDato = regDatoAfterAktivitetsplanen,
                    name = "Navn 2"
                )
            )
            e2.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo("Navn 2")

            val e3 = consumer.processEvent(
                createEvent(
                    Delete,
                    name = "Navn 1"
                )
            )
            e3.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing").row()
                .value("navn").isEqualTo("Navn 1")
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
                val event =
                    consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
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

                val event =
                    consumer.processEvent(createEvent(Insert))

                event.status shouldBe Invalid
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
                val event =
                    consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
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
                    "/api/v1/internal/arena/tiltaksgjennomforing" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                consumer.processEvent(
                    createEvent(
                        Insert,
                        fraDato = "2022-11-11 00:00:00",
                        tilDato = "2023-11-11 00:00:00"
                    )
                )

                val generatedId =
                    engine.requestHistory.last().run {
                        method shouldBe HttpMethod.Put

                        val tiltaksgjennomforing =
                            decodeRequestBody<TiltaksgjennomforingDbo>().apply {
                                tiltakstypeId shouldBe tiltakstype.id
                                tiltaksnummer shouldBe "2022#123"
                                virksomhetsnummer shouldBe "123456"
                                startDato shouldBe LocalDate.of(2022, 11, 11)
                                sluttDato shouldBe LocalDate.of(2023, 11, 11)
                            }

                        tiltaksgjennomforing.id
                    }

                consumer.processEvent(createEvent(Delete))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    decodeRequestBody<TiltaksgjennomforingDbo>().apply {
                        id shouldBe generatedId
                    }
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakgjennomforingEndretConsumer {
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
        deltakere = DeltakerRepository(db)
    )

    return TiltakgjennomforingEndretConsumer(
        ConsumerConfig("tiltakgjennomforing", "tiltakgjennomforing"),
        ArenaEventRepository(db),
        entities,
        client,
        ords
    )
}

private fun createEvent(
    operation: ArenaEventData.Operation,
    tiltakskode: String = "INDOPPFAG",
    name: String = "Navn",
    regDato: String = "2022-10-10 00:00:00",
    fraDato: String? = null,
    tilDato: String? = null
) = createArenaEvent(
    ArenaTables.Tiltaksgjennomforing,
    "3780431",
    operation,
    """{
        "TILTAKGJENNOMFORING_ID": 3780431,
        "LOKALTNAVN": "$name",
        "TILTAKSKODE": "$tiltakskode",
        "ARBGIV_ID_ARRANGOR": 49612,
        "SAK_ID": 13572352,
        "REG_DATO": "$regDato",
        "DATO_FRA": ${fraDato?.let { "\"$fraDato\"" }},
        "DATO_TIL": ${tilDato?.let { "\"$tilDato\"" }},
        "STATUS_TREVERDIKODE_INNSOKNING": "J",
        "ANTALL_DELTAKERE": 5,
        "TILTAKSTATUSKODE": "GJENNOMFOR"
    }"""
)
