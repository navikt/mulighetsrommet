package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.consumers.ArenaTopicConsumer
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository

class TopicServiceTest : FunSpec({
    val topic = "foo-topic"

    val fooEvent = ArenaEvent(
        id = 1,
        status = ArenaEvent.ConsumptionStatus.Processed,
        topic = topic,
        key = "1",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
    )
    val barEvent = ArenaEvent(
        id = 2,
        status = ArenaEvent.ConsumptionStatus.Processed,
        topic = topic,
        key = "2",
        payload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))
    )

    val events = mockk<EventRepository>()
    val consumer = mockk<ArenaTopicConsumer>()

    val service = TopicService(events, ConsumerGroup(listOf(consumer)))

    beforeEach {
        every { consumer.config.topic } returns topic
        coEvery { consumer.replayEvent(any()) } returnsArgument 0
    }

    afterEach {
        clearAllMocks()
    }

    context("replay event") {
        test("should run gracefully when specified event does not exist") {
            every { events.get(1) } returns null

            service.replayEvent(1)

            coVerify(exactly = 0) {
                consumer.replayEvent(any())
            }
        }

        test("should replay event payload specified by id") {
            every { events.get(1) } returns fooEvent

            service.replayEvent(1)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEvent)
            }
        }
    }

    context("replay events") {
        test("should run gracefully when there are no events to replay") {
            every { events.getAll(topic, any(), any()) } returns listOf()

            service.replayEvents(topic)

            coVerify(exactly = 0) {
                consumer.replayEvent(any())
            }
        }

        test("should replay event payload when events are available for the specified topic") {
            every {
                events.getAll(topic, any(), any())
            } returns listOf(
                fooEvent
            ) andThen listOf()

            service.replayEvents(topic)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEvent)
            }
        }

        test("should replay all events in the order of their id") {
            every {
                events.getAll(topic, any(), null)
            } returns listOf(
                fooEvent
            )

            every {
                events.getAll(topic, any(), 1)
            } returns listOf(
                barEvent
            )

            every { events.getAll(topic, any(), 2) } returns listOf()

            service.replayEvents(topic)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEvent)
                consumer.replayEvent(barEvent)
            }
        }

        test("should only replay events after the specified id") {
            every {
                events.getAll(topic, any(), null)
            } returns listOf(
                fooEvent
            )

            every {
                events.getAll(topic, any(), 1)
            } returns listOf(
                barEvent
            )

            every { events.getAll(topic, any(), 2) } returns listOf()

            service.replayEvents(topic, 1)

            coVerify(exactly = 0) {
                consumer.replayEvent(fooEvent)
            }

            coVerify(exactly = 1) {
                consumer.replayEvent(barEvent)
            }
        }
    }
})
