package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingEksternMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.model.Tiltakskode

class SisteTiltaksgjennomforingerV1KafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("migrerte gjennomføringer") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)
        val producer = spyk(
            ArenaMigreringTiltaksgjennomforingerV1KafkaProducer(
                producerClient,
                config = ArenaMigreringTiltaksgjennomforingerV1KafkaProducer.Config(topic = "topic"),
            ),
        )

        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
        ).initialize(database.db)

        val (gjennomforing, endretTidspunkt) = database.run {
            val tiltak = queries.gjennomforing.get(GjennomforingFixtures.Oppfolging1.id).shouldNotBeNull()
            val ts = queries.gjennomforing.getUpdatedAt(GjennomforingFixtures.Oppfolging1.id)
            Pair(tiltak, ts)
        }

        fun createConsumer(
            tiltakstyper: TiltakstypeService,
            arenaAdapterClient: ArenaAdapterClient,
        ) = SisteTiltaksgjennomforingerV1KafkaConsumer(
            KafkaTopicConsumer.Config(id = "id", topic = "topic"),
            database.db,
            tiltakstyper,
            producer,
            arenaAdapterClient,
        )

        afterEach {
            clearAllMocks()
        }

        test("skal ikke publisere gjennomføringer til migreringstopic før tiltakstype er migrert") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns null

            val tiltakstyper = TiltakstypeService(database.db, enabledTiltakskoder = emptyList())

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

            verify(exactly = 0) { producer.publish(any()) }
            verify(exactly = 0) { producerClient.sendSync(any()) }
        }

        test("skal publisere gjennomføringer til tiltaksgjennomføringer når tiltakstype er migrert") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns null

            val tiltakstyper = TiltakstypeService(database.db, listOf(Tiltakskode.OPPFOLGING))

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

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

            val tiltakstyper = TiltakstypeService(database.db, listOf(Tiltakskode.OPPFOLGING))

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

            val expectedMessage = ArenaMigreringTiltaksgjennomforingDto.from(gjennomforing, 123, endretTidspunkt)
            verify(exactly = 1) { producer.publish(expectedMessage) }
            verify(exactly = 1) { producerClient.sendSync(any()) }
        }
    }
})

private suspend fun consumeGjennomforing(
    consumer: SisteTiltaksgjennomforingerV1KafkaConsumer,
    gjennomforing: GjennomforingDto,
) {
    val message = TiltaksgjennomforingEksternMapper.toTiltaksgjennomforingV1Dto(gjennomforing)
    consumer.consume(gjennomforing.id.toString(), Json.encodeToJsonElement(message))
}
