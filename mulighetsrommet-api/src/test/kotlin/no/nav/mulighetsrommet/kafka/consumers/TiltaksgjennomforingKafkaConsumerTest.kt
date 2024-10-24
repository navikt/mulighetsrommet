package no.nav.mulighetsrommet.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.clients.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.producers.ArenaMigreringTiltaksgjennomforingerV1KafkaProducer

class TiltaksgjennomforingKafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("migrerte gjennomføringer") {
        val producerClient = mockk<KafkaProducerClient<String, String?>>(relaxed = true)
        val producer = spyk(
            ArenaMigreringTiltaksgjennomforingerV1KafkaProducer(
                producerClient,
                config = ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config(topic = "topic"),
            ),
        )

        val gjennomforinger = TiltaksgjennomforingRepository(database.db)

        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
        ).initialize(database.db)

        val gjennomforing = gjennomforinger.get(TiltaksgjennomforingFixtures.Oppfolging1.id)
        gjennomforing.shouldNotBeNull()

        val endretTidspunkt = gjennomforinger.getUpdatedAt(gjennomforing.id)
        endretTidspunkt.shouldNotBeNull()

        afterEach {
            clearAllMocks()
        }

        test("skal publisere gjennomføringer til migreringstopic") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns null

            val consumer = SisteTiltaksgjennomforingerV1KafkaConsumer(
                KafkaTopicConsumer.Config(id = "id", topic = "topic"),
                gjennomforinger,
                producer,
                arenaAdapterClient,
            )

            consumer.consume(
                gjennomforing.id.toString(),
                Json.encodeToJsonElement(gjennomforing.toTiltaksgjennomforingV1Dto()),
            )

            val expectedMessage = ArenaMigreringTiltaksgjennomforingDto.from(gjennomforing, null, endretTidspunkt)
            verify(exactly = 1) { producer.publish(expectedMessage) }
            verify(exactly = 1) { producerClient.sendSync(any()) }
        }

        test("skal inkludere eksisterende arenaId når gjennomføring allerede eksisterer i Arena") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns ArenaTiltaksgjennomforingDto(
                arenaId = 123,
                status = "AVSLU",
            )

            val consumer = SisteTiltaksgjennomforingerV1KafkaConsumer(
                KafkaTopicConsumer.Config(id = "id", topic = "topic"),
                gjennomforinger,
                producer,
                arenaAdapterClient,
            )

            consumer.consume(
                gjennomforing.id.toString(),
                Json.encodeToJsonElement(gjennomforing.toTiltaksgjennomforingV1Dto()),
            )

            val expectedMessage = ArenaMigreringTiltaksgjennomforingDto.from(gjennomforing, 123, endretTidspunkt)
            verify(exactly = 1) { producer.publish(expectedMessage) }
            verify(exactly = 1) { producerClient.sendSync(any()) }
        }
    }
})
