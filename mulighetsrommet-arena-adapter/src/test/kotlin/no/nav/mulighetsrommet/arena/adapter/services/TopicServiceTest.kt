package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerGroup
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.Event
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository

class TopicServiceTest : FunSpec({
    val topic = "foo-topic"

    val fooEventPayload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
    val barEventPayload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))

    val events = mockk<EventRepository>()
    val consumer = mockk<TopicConsumer<Any>>()

    val service = TopicService(events, ConsumerGroup(listOf(consumer)))

    beforeEach {
        every { consumer.consumerConfig.topic } answers { topic }
        coEvery { consumer.replayEvent(any()) } just runs
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
            every { events.get(1) } returns Event(
                id = 1,
                status = Event.ConsumptionStatus.Processed,
                topic = topic,
                payload = fooEventPayload.toString()
            )

            service.replayEvent(1)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
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
                Event(
                    id = 1,
                    status = Event.ConsumptionStatus.Processed,
                    topic = topic,
                    payload = fooEventPayload.toString()
                )
            ) andThen listOf()

            service.replayEvents(topic)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
            }
        }

        test("should replay all events in the order of their id") {
            every {
                events.getAll(topic, any(), null)
            } returns listOf(
                Event(
                    id = 1,
                    status = Event.ConsumptionStatus.Processed,
                    topic = topic,
                    payload = fooEventPayload.toString()
                )
            )

            every {
                events.getAll(topic, any(), 1)
            } returns listOf(
                Event(
                    id = 2,
                    status = Event.ConsumptionStatus.Processed,
                    topic = topic,
                    payload = barEventPayload.toString()
                )
            )

            every { events.getAll(topic, any(), 2) } returns listOf()

            service.replayEvents(topic)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
                consumer.replayEvent(barEventPayload)
            }
        }

        test("should only replay events after the specified id") {
            every {
                events.getAll(topic, any(), null)
            } returns listOf(
                Event(
                    id = 1,
                    status = Event.ConsumptionStatus.Processed,
                    topic = topic,
                    payload = fooEventPayload.toString()
                )
            )

            every {
                events.getAll(topic, any(), 1)
            } returns listOf(
                Event(
                    id = 2,
                    status = Event.ConsumptionStatus.Processed,
                    topic = topic,
                    payload = barEventPayload.toString()
                )
            )

            every { events.getAll(topic, any(), 2) } returns listOf()

            service.replayEvents(topic, 1)

            coVerify(exactly = 0) {
                consumer.replayEvent(fooEventPayload)
            }

            coVerify(exactly = 1) {
                consumer.replayEvent(barEventPayload)
            }
        }
    }
})
