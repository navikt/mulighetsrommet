package no.nav.mulighetsrommet.arena.adapter.consumers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.MulighetsrommetApiClient
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import no.nav.mulighetsrommet.domain.adapter.AdapterSak

class SakEndretConsumerTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    beforeEach {
        listener.db.migrate()
    }

    afterEach {
        listener.db.clean()
    }

    context("when sakskode is not TILT") {
        test("should ignore events") {
            val engine = MockEngine { respondOk() }

            val event = createConsumer(listener.db, engine).processEvent(createEvent(sakskode = "NOT_TILT"))

            event.status shouldBe ArenaEvent.ConsumptionStatus.Ignored

            engine.requestHistory shouldHaveSize 0
        }
    }

    context("when sakskode is TILT") {
        test("should call api with mapped event payload") {
            val engine = MockEngine { respondOk() }

            val event = createConsumer(listener.db, engine).processEvent(createEvent())

            event.status shouldBe ArenaEvent.ConsumptionStatus.Processed

            engine.requestHistory.last().run {
                method shouldBe HttpMethod.Put

                decodeRequestBody<AdapterSak>() shouldBe AdapterSak(
                    sakId = 1,
                    aar = 2022,
                    lopenummer = 2,
                )
            }
        }

        context("when api returns 409") {
            test("should treat the result as successful") {
                val engine = MockEngine { respondError(HttpStatusCode.Conflict) }

                createConsumer(listener.db, engine).processEvent(createEvent())

                engine.requestHistory.last().run {
                    method shouldBe HttpMethod.Put

                    decodeRequestBody<AdapterSak>() shouldBe AdapterSak(
                        sakId = 1,
                        aar = 2022,
                        lopenummer = 2,
                    )
                }
            }
        }

        context("when api returns 500") {
            test("should treat the result as error") {
                val consumer = createConsumer(
                    listener.db,
                    MockEngine { respondError(HttpStatusCode.InternalServerError) }
                )

                shouldThrow<ResponseException> {
                    consumer.processEvent(createEvent())
                }
            }
        }
    }
})

private fun createConsumer(db: Database, engine: HttpClientEngine): SakEndretConsumer {
    val client = MulighetsrommetApiClient(engine, maxRetries = 0, baseUri = "api") {
        "Bearer token"
    }

    return SakEndretConsumer(
        ConsumerConfig("sakendret", "sakendret"),
        ArenaEventRepository(db),
        client
    )
}

private fun createEvent(sakskode: String = "TILT") = createArenaInsertEvent(
    "sak",
    "1",
    """{
        "SAK_ID": 1,
        "SAKSKODE": "$sakskode",
        "AAR": 2022,
        "LOPENRSAK": 2
    }"""
)
