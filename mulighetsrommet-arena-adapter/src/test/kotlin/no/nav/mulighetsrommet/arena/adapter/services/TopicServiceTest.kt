package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.kafka.ConsumerSetup
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.Event
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository

class TopicServiceTest : FunSpec({

    context("replay events") {
        val topic = "foo-topic"

        val fooEventPayload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
        val barEventPayload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))

        val events = mockk<EventRepository>()
        val consumer = mockk<TopicConsumer<Any>>()

        val service = TopicService(events, ConsumerSetup(listOf(consumer)))

        beforeEach {
            every { consumer.consumerConfig.topic } answers { topic }
            coEvery { consumer.replayEvent(any()) } just runs
        }

        afterEach {
            clearAllMocks()
        }

        test("should run gracefully when there are no events to replay") {
            every { events.getEvents(topic, any(), any()) } returns listOf()

            service.replayEvents(topic)

            coVerify(exactly = 0) {
                consumer.replayEvent(any())
            }
        }

        test("should replay event payload when events are available for the specified topic") {
            every {
                events.getEvents(topic, any(), any())
            } returns listOf(
                Event(id = 1, payload = fooEventPayload.toString())
            ) andThen listOf()

            service.replayEvents(topic)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
            }
        }

        test("should replay all events in the order of their id") {
            every {
                events.getEvents(topic, any(), null)
            } returns listOf(
                Event(id = 1, payload = fooEventPayload.toString())
            )

            every {
                events.getEvents(topic, any(), 1)
            } returns listOf(
                Event(id = 2, payload = barEventPayload.toString())
            )

            every { events.getEvents(topic, any(), 2) } returns listOf()

            service.replayEvents(topic)

            coVerify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
                consumer.replayEvent(barEventPayload)
            }
        }

        test("should only replay events after the specified id") {
            every {
                events.getEvents(topic, any(), null)
            } returns listOf(
                Event(id = 1, payload = fooEventPayload.toString())
            )

            every {
                events.getEvents(topic, any(), 1)
            } returns listOf(
                Event(id = 2, payload = barEventPayload.toString())
            )

            every { events.getEvents(topic, any(), 2) } returns listOf()

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
