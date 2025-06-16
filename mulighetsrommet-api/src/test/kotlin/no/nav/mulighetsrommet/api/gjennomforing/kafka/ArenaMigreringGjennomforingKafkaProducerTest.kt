package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
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
import no.nav.mulighetsrommet.model.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.model.Tiltakskode
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

class ArenaMigreringGjennomforingKafkaProducerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("migrerte gjennomføringer") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
        ).initialize(database.db)

        val gjennomforing = database.run {
            queries.gjennomforing.get(GjennomforingFixtures.Oppfolging1.id).shouldNotBeNull()
        }

        fun createConsumer(
            tiltakstyper: TiltakstypeService,
            arenaAdapterClient: ArenaAdapterClient,
        ) = ArenaMigreringGjennomforingKafkaProducer(
            ArenaMigreringGjennomforingKafkaProducer.Config(
                producerTopic = "producer-topic",
            ),
            database.db,
            tiltakstyper,
            arenaAdapterClient,
            producerClient,
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

            verify(exactly = 0) { producerClient.sendSync(any()) }
        }

        test("skal publisere gjennomføringer til tiltaksgjennomføringer når tiltakstype er migrert") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns null

            val tiltakstyper = TiltakstypeService(database.db, listOf(Tiltakskode.OPPFOLGING))

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

            val expectedMessage = ArenaMigreringTiltaksgjennomforingDto.from(gjennomforing, null)
            verify(exactly = 1) {
                producerClient.sendSync(
                    match { expectKafkaMessage(it, expectedKey = gjennomforing.id, expectedMessage = expectedMessage) },
                )
            }
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

            val expectedMessage = ArenaMigreringTiltaksgjennomforingDto.from(gjennomforing, 123)
            verify(exactly = 1) {
                producerClient.sendSync(
                    match { expectKafkaMessage(it, expectedKey = gjennomforing.id, expectedMessage = expectedMessage) },
                )
            }
        }
    }
})

private fun expectKafkaMessage(
    record: ProducerRecord<ByteArray, ByteArray?>,
    expectedKey: UUID,
    expectedMessage: ArenaMigreringTiltaksgjennomforingDto,
): Boolean = checkEquals(record.topic(), "producer-topic") &&
    checkEquals(record.key().decodeToString(), expectedKey.toString()) &&
    checkEquals(Json.decodeFromString(record.value()!!.decodeToString()), expectedMessage)

private suspend fun consumeGjennomforing(
    consumer: ArenaMigreringGjennomforingKafkaProducer,
    gjennomforing: GjennomforingDto,
) {
    val message = TiltaksgjennomforingEksternMapper.fromGjennomforingDto(gjennomforing)
    consumer.consume(gjennomforing.id.toString(), Json.encodeToJsonElement(message))
}

private fun <T> checkEquals(a: T, b: T): Boolean {
    check(a == b) {
        "Expected '$a' to be equal to '$b'"
    }

    return true
}
