package no.nav.mulighetsrommet.api.gjennomforing.task

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingDetaljerService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

class InitialLoadGjennomforingerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(
            GjennomforingFixtures.Oppfolging1,
            GjennomforingFixtures.ArenaEnkelAmo,
            GjennomforingFixtures.EnkelAmo,
            GjennomforingFixtures.ArenaArbeidsrettetRehabilitering,
        ),
    )

    beforeSpec {
        domain.initialize(database.db)
    }

    val gjennomforinvV2Topic = "topic-v2"

    fun createTask(
        database: ApiDatabaseTestListener,
        producerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    ): InitialLoadGjennomforinger = InitialLoadGjennomforinger(
        InitialLoadGjennomforinger.Config(gjennomforinvV2Topic),
        database.db,
        GjennomforingDetaljerService(database.db, TiltakstypeService(db = database.db), mockk()),
        producerClient,
    )

    test("initial load basert på tiltakskode") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val task = createTask(database, producerClient)

        task.initialLoadByTiltakskode(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING)

        verify(exactly = 2) { producerClient.sendSync(any()) }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Enkeltplass(GjennomforingFixtures.ArenaEnkelAmo.id)
                },
            )
        }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Enkeltplass(GjennomforingFixtures.EnkelAmo.id)
                },
            )
        }

        task.initialLoadByTiltakskode(Tiltakskode.OPPFOLGING)

        verify(exactly = 3) { producerClient.sendSync(any()) }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Gruppetiltak(GjennomforingFixtures.Oppfolging1.id)
                },
            )
        }
    }

    test("initial load basert på id") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val task = createTask(database, producerClient)

        task.initialLoadGjennomforingerById(listOf(UUID.randomUUID()))

        verify(exactly = 0) { producerClient.sendSync(any()) }

        task.initialLoadGjennomforingerById(
            listOf(GjennomforingFixtures.EnkelAmo.id, GjennomforingFixtures.ArenaEnkelAmo.id),
        )

        verify(exactly = 2) { producerClient.sendSync(any()) }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Enkeltplass(GjennomforingFixtures.ArenaEnkelAmo.id)
                },
            )
        }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Enkeltplass(GjennomforingFixtures.EnkelAmo.id)
                },
            )
        }

        task.initialLoadGjennomforingerById(
            listOf(GjennomforingFixtures.Oppfolging1.id, GjennomforingFixtures.ArenaArbeidsrettetRehabilitering.id),
        )

        verify(exactly = 4) { producerClient.sendSync(any()) }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Gruppetiltak(GjennomforingFixtures.Oppfolging1.id)
                },
            )
        }
        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltaksgjennomforingV2Gruppetiltak(GjennomforingFixtures.ArenaArbeidsrettetRehabilitering.id)
                },
            )
        }
    }
})

private fun ProducerRecord<ByteArray, ByteArray?>.shouldBeTiltaksgjennomforingV2Gruppetiltak(id: UUID): Boolean {
    val decoded = value()?.let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it.decodeToString()) }
    return key().decodeToString() == id.toString() &&
        decoded != null &&
        decoded is TiltaksgjennomforingV2Dto.Gruppe
}

private fun ProducerRecord<ByteArray, ByteArray?>.shouldBeTiltaksgjennomforingV2Enkeltplass(id: UUID): Boolean {
    val decoded = value()?.let { Json.decodeFromString<TiltaksgjennomforingV2Dto>(it.decodeToString()) }
    return key().decodeToString() == id.toString() &&
        decoded != null &&
        decoded is TiltaksgjennomforingV2Dto.Enkeltplass
}
