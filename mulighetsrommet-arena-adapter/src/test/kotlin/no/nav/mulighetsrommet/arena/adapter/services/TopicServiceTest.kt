package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.Event
import no.nav.mulighetsrommet.arena.adapter.repositories.EventRepository
import java.time.LocalDateTime

class TopicServiceTest : FunSpec({

    context("replay events") {
        val topic = "foo-topic"

        val fooEventPayload = JsonObject(mapOf("name" to JsonPrimitive("Foo")))
        val barEventPayload = JsonObject(mapOf("name" to JsonPrimitive("Bar")))

        val events = mockk<EventRepository>()
        val consumer = mockk<TopicConsumer<Any>>()

        val service = TopicService(events, listOf(consumer))

        beforeEach {
            every { consumer.topic } answers { topic }
            every { consumer.replayEvent(any()) } just runs
        }

        afterEach {
            clearAllMocks()
        }

        test("should run gracefully when there are no events to replay") {
            every { events.getEvents(topic, any(), any()) } returns listOf()

            service.replayEvents(topic)

            verify(exactly = 0) {
                consumer.replayEvent(any())
            }
        }

        test("should replay event payload when events are available for the specified topic") {
            every {
                events.getEvents(topic, any(), any())
            } returns listOf(
                Event(
                    id = 1,
                    payload = fooEventPayload.toString(),
                    createdAt = LocalDateTime.parse("2022-06-01T00:00:00")
                )
            ) andThen listOf()

            service.replayEvents(topic)

            verify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
            }
        }

        test("should replay all events in the order of their creation date") {
            val firstEventCreatedAt = LocalDateTime.parse("2022-06-01T00:00:00")
            every {
                events.getEvents(topic, any(), null)
            } returns listOf(
                Event(id = 1, payload = fooEventPayload.toString(), createdAt = firstEventCreatedAt)
            )

            val secondEventCreatedAt = LocalDateTime.parse("2022-06-02T00:00:00")
            every {
                events.getEvents(topic, any(), firstEventCreatedAt)
            } returns listOf(
                Event(id = 2, payload = barEventPayload.toString(), createdAt = secondEventCreatedAt)
            )

            every { events.getEvents(topic, any(), secondEventCreatedAt) } returns listOf()

            service.replayEvents(topic)

            verify(exactly = 1) {
                consumer.replayEvent(fooEventPayload)
                consumer.replayEvent(barEventPayload)
            }
        }

        test("should only replay events created after the specified creation date") {
            val firstEventCreatedAt = LocalDateTime.parse("2022-06-01T00:00:00")
            every {
                events.getEvents(topic, any(), null)
            } returns listOf(
                Event(id = 1, payload = fooEventPayload.toString(), createdAt = firstEventCreatedAt)
            )

            val secondEventCreatedAt = LocalDateTime.parse("2022-06-02T00:00:00")
            every {
                events.getEvents(topic, any(), firstEventCreatedAt)
            } returns listOf(
                Event(id = 2, payload = barEventPayload.toString(), createdAt = secondEventCreatedAt)
            )

            every { events.getEvents(topic, any(), secondEventCreatedAt) } returns listOf()

            service.replayEvents(topic, firstEventCreatedAt)

            verify(exactly = 0) {
                consumer.replayEvent(fooEventPayload)
            }

            verify(exactly = 1) {
                consumer.replayEvent(barEventPayload)
            }
        }
    }
})
