package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.consumers.ArenaTopicConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent.ConsumptionStatus
import no.nav.mulighetsrommet.arena.adapter.repositories.ArenaEventRepository

class ArenaEventServiceTest : FunSpec({
    val table = "foo"

    val fooEvent = ArenaEvent(
        status = ConsumptionStatus.Processed,
        arenaTable = table,
        arenaId = "1",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
    )
    val barEvent = ArenaEvent(
        status = ConsumptionStatus.Processed,
        arenaTable = table,
        arenaId = "2",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))
    )

    val arenaData = mockk<ArenaEventRepository>()
    val consumer = mockk<ArenaTopicConsumer>()

    val service = ArenaEventService(arenaData, ConsumerGroup(listOf(consumer)))

    beforeEach {
        every { consumer.arenaTable } returns table
        coEvery { consumer.replayEvent(any()) } returnsArgument 0
    }

    afterEach {
        clearAllMocks()
    }

    context("replay event") {
        test("should run gracefully when specified event does not exist") {
            every { arenaData.get(table, "1") } returns null

            service.replayEvent(table, "1")

            coVerify(exactly = 0) {
                consumer.replayEvent(any())
            }
        }

        test("should replay event payload specified by id") {
            every { arenaData.get(table, "1") } returns fooEvent

            service.replayEvent(table, "1")

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEvent)
            }
        }
    }

    context("replay events") {
        test("should run gracefully when there are no events to replay") {
            every { arenaData.getAll(table, any(), any()) } returns listOf()

            service.replayEvents(table)

            coVerify(exactly = 0) {
                consumer.replayEvent(any())
            }
        }

        test("should replay event payload when events are available for the specified table") {
            every {
                arenaData.getAll(table, any(), any(), any())
            } returns listOf(
                fooEvent
            ) andThen listOf()

            service.replayEvents(table)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEvent)
            }
        }

        test("should replay all events in order") {
            every { arenaData.getAll(table, ConsumptionStatus.Processed, 1, 0) } returns listOf(fooEvent)

            every { arenaData.getAll(table, ConsumptionStatus.Processed, 1, 1) } returns listOf(barEvent)

            every { arenaData.getAll(table, ConsumptionStatus.Processed, 1, 2) } returns listOf()

            service.replayEvents(table, ConsumptionStatus.Processed)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEvent)
                consumer.replayEvent(barEvent)
            }
        }
    }
})
