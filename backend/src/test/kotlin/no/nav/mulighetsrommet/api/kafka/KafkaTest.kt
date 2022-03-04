package no.nav.mulighetsrommet.api.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.domain.Tiltakstype
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class KafkaTest : FunSpec({

    val topicMap = mapOf(Pair("tiltakendret", "topic-tiltakendret"))
    val mockTiltakstypeService = mockk<TiltakstypeService>()
    val eventProcessor = EventProcessor(topicMap, mockTiltakstypeService)
    val kafkaFactory = KafkaFactory(mockTiltakstypeService)

    afterTest {
        clearMocks(mockTiltakstypeService)
    }

    context("KafkaFactory") {
        test("should return the correct topic map") {
            kafkaFactory.getConsumerTopics().shouldContainValues("teamarenanais.aapen-arena-tiltakendret-v1-q2")
        }
    }

    context("EventProcessor") {
        test("should invoke the correct handler function") {
            val expectedTiltakstype = Tiltakstype(
                null,
                "2-årig opplæringstiltak",
                1,
                null,
                Tiltakskode.MENTOR,
                LocalDateTime.parse("2016-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                LocalDateTime.parse("2019-06-30 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "SIAMO",
                LocalDateTime.parse("2015-12-30 08:42:06", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            )
            coEvery { mockTiltakstypeService.createTiltakstype(any()) } returns expectedTiltakstype
            val record = ConsumerRecord("topic-tiltakendret", 0, 0, "MENTOR", tiltakEndretMentor)
            eventProcessor.process(record)
            coVerify(exactly = 1) { mockTiltakstypeService.createTiltakstype(expectedTiltakstype) }
        }
    }
})
