package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.clients.ArenaOrdsProxyClientImpl
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData
import no.nav.mulighetsrommet.arena.adapter.models.ArenaEventData.Operation.*
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTables
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus.*
import no.nav.mulighetsrommet.arena.adapter.models.db.Sak
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltaksgjennomforing
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.models.dto.ArenaOrdsFnr
import no.nav.mulighetsrommet.arena.adapter.repositories.*
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.decodeRequestBody
import no.nav.mulighetsrommet.ktor.respondJson
import java.util.*

class TiltakdeltakerEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        database.db.migrate()
    }

    afterEach {
        database.db.clean()
    }

    context("when dependent events has not been processed") {
        test("should save the event with status Failed when the dependent tiltaksgjennomføring is missing") {
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert))

            event.status shouldBe Failed
            database.assertThat("deltaker").isEmpty
        }
    }

    context("when dependent events has been processed") {
        val tiltakstype = Tiltakstype(
            id = UUID.randomUUID(),
            navn = "Oppfølging",
            tiltakskode = "INDOPPFAG"
        )
        val sak = Sak(
            sakId = 1,
            lopenummer = 123,
            aar = 2022,
            enhet = "2990"
        )
        val tiltaksgjennomforing = Tiltaksgjennomforing(
            id = UUID.randomUUID(),
            tiltaksgjennomforingId = 3,
            sakId = 1,
            tiltakskode = "INDOPPFAG",
            arrangorId = null,
            navn = null,
            status = "GJENNOMFOR"
        )

        beforeEach {
            val tiltakstyper = TiltakstypeRepository(database.db)
            tiltakstyper.upsert(tiltakstype)

            val saker = SakRepository(database.db)
            saker.upsert(sak)

            val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforinger.upsert(tiltaksgjennomforing)

            val mappings = ArenaEntityMappingRepository(database.db)
            mappings.insert(
                ArenaEntityMapping(
                    ArenaTables.Tiltaksgjennomforing,
                    tiltaksgjennomforing.tiltaksgjennomforingId.toString(),
                    tiltaksgjennomforing.id
                )
            )

            val events = ArenaEventRepository(database.db)
            events.upsert(
                createArenaEvent(
                    ArenaTables.Tiltaksgjennomforing,
                    tiltaksgjennomforing.tiltaksgjennomforingId.toString(),
                    operation = Insert,
                    data = Json.encodeToString(tiltaksgjennomforing),
                    status = Processed
                )
            )
        }

        test("should be ignored when dependent tiltaksgjennomforing is ignored") {
            val events = ArenaEventRepository(database.db)
            events.upsert(
                createArenaEvent(
                    ArenaTables.Tiltaksgjennomforing,
                    tiltaksgjennomforing.tiltaksgjennomforingId.toString(),
                    operation = Insert,
                    data = Json.encodeToString(tiltaksgjennomforing),
                    status = Ignored
                )
            )
            val consumer = createConsumer(database.db, MockEngine { respondOk() })

            val event = consumer.processEvent(createEvent(Insert, status = "FULLF"))

            event.status shouldBe Ignored
        }

        test("should treat all operations as upserts") {
            val engine = createMockEngine(
                "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                "/api/v1/internal/arena/deltaker" to { respondOk() }
            )
            val consumer = createConsumer(database.db, engine)

            val e1 = consumer.processEvent(createEvent(Insert, status = "GJENN"))
            e1.status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("DELTAR")

            val e2 = consumer.processEvent(createEvent(Update, status = "FULLF"))
            e2.status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("AVSLUTTET")

            val e3 = consumer.processEvent(createEvent(Delete, status = "FULLF"))
            e3.status shouldBe Processed
            database.assertThat("deltaker")
                .row().value("status").isEqualTo("AVSLUTTET")
        }

        context("api responses") {
            test("should mark the event as Failed when arena ords proxy responds with an error") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondError(HttpStatusCode.InternalServerError) },
                    "/api/v1/internal/arena/deltaker" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
            }

            // TODO: burde manglende data i ords ha en annen semantikk enn Invalid?
            test("should mark the event as Invalid when arena ords proxy responds with NotFound") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondError(HttpStatusCode.NotFound) },
                    "/api/v1/internal/arena/deltaker" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Invalid
            }

            test("should mark the event as Failed when api responds with an error") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/deltaker" to { respondError(HttpStatusCode.InternalServerError) }
                )

                val consumer = createConsumer(database.db, engine)

                val event = consumer.processEvent(createEvent(Insert))

                event.status shouldBe Failed
            }

            test("should call api with mapped event payload when all services responds with success") {
                val engine = createMockEngine(
                    "/ords/fnr" to { respondJson(ArenaOrdsFnr("12345678910")) },
                    "/api/v1/internal/arena/deltaker" to { respondOk() }
                )

                val consumer = createConsumer(database.db, engine)

                consumer.processEvent(createEvent(Insert))

                val generatedId = engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    val deltaker = decodeRequestBody<DeltakerDbo>().apply {
                        tiltaksgjennomforingId shouldBe tiltaksgjennomforing.id
                        norskIdent shouldBe "12345678910"
                    }

                    deltaker.id
                }

                consumer.processEvent(createEvent(Delete))

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Delete

                    decodeRequestBody<DeltakerDbo>().apply {
                        id shouldBe generatedId
                    }
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): TiltakdeltakerEndretConsumer {
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

    return TiltakdeltakerEndretConsumer(
        ConsumerConfig("deltaker", "deltaker"),
        ArenaEventRepository(db),
        entities,
        client,
        ords,
    )
}

private fun createEvent(operation: ArenaEventData.Operation, status: String = "GJENN") = createArenaEvent(
    ArenaTables.Deltaker,
    "1",
    operation,
    """{
        "TILTAKDELTAKER_ID": 1,
        "PERSON_ID": 2,
        "TILTAKGJENNOMFORING_ID": 3,
        "DELTAKERSTATUSKODE": "$status",
        "DATO_FRA": null,
        "DATO_TIL": null
    }"""
)
