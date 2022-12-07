package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
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
import no.nav.mulighetsrommet.arena.adapter.models.dto.Arrangor
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.models.Tiltaksgjennomforing
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

class TiltakgjennomforingEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when dependent tiltakstype is missing") {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(
                Tiltakstype(
                    id = UUID.randomUUID(),
                    navn = "Oppfølging",
                    tiltakskode = "INDOPPFAG"
                )
            )

            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("tiltaksgjennomforing").isEmpty
        }

        test("should save the event with status Failed when dependent sak is missing") {
            val saker = SakRepository(database.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("tiltaksgjennomforing").isEmpty
        }
    }

    context("when dependent events has been processed") {
        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakskode = "INDOPPFAG"
        )

        beforeEach {
            val saker = SakRepository(database.db)
            saker.upsert(Sak(sakId = 13572352, lopenummer = 123, aar = 2022))

            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype)

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.insert(
                ArenaEntityMapping(
                    ArenaTables.Tiltakstype,
                    tiltakstype.tiltakskode,
                    tiltakstype.id
                )
            )
        }

        test("should ignore tiltaksgjennomføringer older than Aktivitetsplanen") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert, regDato = "2017-12-03 23:59:59"))

            event.status shouldBe Ignored
        }

        test("should treat all operations as upserts") {
            val engine = createMockEngine(
                "/ords/arbeidsgiver" to { respondJson(Arrangor("123456", "000000")) },
                "/api/v1/arena/tiltaksgjennomforing" to { respondOk() },
            )

            val consumer = createConsumer(database.db, engine)

            val e1 = consumer.processEvent(createEvent(Insert, name = "Navn 1"))
            e1.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing")
                .row().value("navn").isEqualTo("Navn 1")

            val e2 = consumer.processEvent(createEvent(Update, name = "Navn 2"))
            e2.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing")
                .row().value("navn").isEqualTo("Navn 2")

            val e3 = consumer.processEvent(createEvent(Delete, name = "Navn 1"))
            e3.status shouldBe Processed
            database.assertThat("tiltaksgjennomforing")
                .row().value("navn").isEqualTo("Navn 1")
        }

        context("api responses") {
            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to { respondError(HttpStatusCode.InternalServerError) },
                )

                val consumer = createConsumer(database.db, engine)
                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to { respondError(HttpStatusCode.NotFound) },
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Invalid
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to { respondJson(Arrangor("123456", "000000")) },
                    "/api/v1/arena/tiltaksgjennomforing" to { respondError(HttpStatusCode.InternalServerError) },
                )

                val consumer = createConsumer(database.db, engine)
                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
            }

            test("should call api with mapped event payload when all services responds with success") {
                val engine = createMockEngine(
                    "/ords/arbeidsgiver" to { respondJson(Arrangor("123456", "000000")) },
                    "/api/v1/arena/tiltaksgjennomforing" to { respondOk() },
                )

                val consumer = createConsumer(database.db, engine)

                consumer.processEvent(createEvent(Insert))

                val generatedId = engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    val tiltaksgjennomforing = decodeRequestBody<Tiltaksgjennomforing>().apply {
                        tiltakstypeId shouldBe tiltakstype.id
                        tiltaksnummer shouldBe "123"
                        virksomhetsnummer shouldBe "123456"
                    }

                    tiltaksgjennomforing.id
                }

                consumer.processEvent(createEvent(Delete))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    decodeRequestBody<Tiltaksgjennomforing>().apply {
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
        deltakere = DeltakerRepository(db),
    )

    return TiltakgjennomforingEndretConsumer(
        ConsumerConfig("tiltakgjennomforing", "tiltakgjennomforing"),
        ArenaEventRepository(db),
        entities,
        client,
        ords,
    )
}

private fun createEvent(
    operation: ArenaEventData.Operation,
    name: String = "Navn",
    regDato: String = "2022-10-10 00:00:00"
) = createArenaEvent(
    ArenaTables.Tiltaksgjennomforing,
    "3780431",
    operation,
    """{
        "TILTAKGJENNOMFORING_ID": 3780431,
        "LOKALTNAVN": "$name",
        "TILTAKSKODE": "INDOPPFAG",
        "ARBGIV_ID_ARRANGOR": 49612,
        "SAK_ID": 13572352,
        "REG_DATO": "$regDato",
        "DATO_FRA": null,
        "DATO_TIL": null,
        "STATUS_TREVERDIKODE_INNSOKNING": "J",
        "ANTALL_DELTAKERE": 5
    }"""
)
