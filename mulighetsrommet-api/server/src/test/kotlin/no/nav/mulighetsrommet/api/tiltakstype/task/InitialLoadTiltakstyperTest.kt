package no.nav.mulighetsrommet.api.tiltakstype.task

import io.kotest.core.spec.style.FunSpec
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.contracts.tiltakstype.TiltakstypeV3Dto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Innsatsgruppe
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

class InitialLoadTiltakstyperTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val sanityIdOppfolging = UUID.randomUUID()
    val sanityIdArbeidsmedStotte = UUID.randomUUID()

    val domain = MulighetsrommetTestDomain(
        tiltakstyper = listOf(
            TiltakstypeFixtures.Oppfolging,
            TiltakstypeFixtures.IPS,
            TiltakstypeFixtures.Arbeidstrening,
        ),
    ) {
        repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging.copy(sanityId = sanityIdOppfolging, innsatsgrupper = setOf(Innsatsgruppe.TRENGER_VEILEDNING)))
        repository.tiltakstype.save(TiltakstypeFixtures.IPS.copy(sanityId = sanityIdArbeidsmedStotte))
    }

    beforeSpec {
        domain.initialize(database.api)
    }

    val kafkaTopic = "test-tiltakstype-topic"

    fun createTask(
        producerClient: KafkaProducerClient<ByteArray, ByteArray?>,
        sanityService: SanityService,
    ): InitialLoadTiltakstyper = InitialLoadTiltakstyper(
        InitialLoadTiltakstyper.Config(kafkaTopic),
        database.api,
        producerClient,
        sanityService,
    )

    test("publiseres til Kafka og når system er TILTAKSADMINISTRASJON") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)
        val sanityService = mockk<SanityService>(relaxed = true)

        val task = createTask(producerClient, sanityService)

        task.initialLoadTiltakstyper()

        verify(exactly = 1) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltakstypeV3(kafkaTopic, TiltakstypeFixtures.Oppfolging.id)
                },
            )
        }
        verify(exactly = 0) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltakstypeV3(kafkaTopic, TiltakstypeFixtures.IPS.id)
                },
            )
        }
        verify(exactly = 0) {
            producerClient.sendSync(
                match { record ->
                    record.shouldBeTiltakstypeV3(kafkaTopic, TiltakstypeFixtures.Arbeidstrening.id)
                },
            )
        }
    }

    test("publiseres til sanity når gjennomføring har sanityId") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)
        val sanityService = mockk<SanityService>(relaxed = true)

        val task = createTask(producerClient, sanityService)

        task.initialLoadTiltakstyper()

        coVerify(exactly = 1) {
            sanityService.patchSanityTiltakstype(
                sanityIdOppfolging,
                TiltakstypeFixtures.Oppfolging.navn,
            )
        }
        coVerify(exactly = 1) {
            sanityService.patchSanityTiltakstype(
                sanityIdArbeidsmedStotte,
                TiltakstypeFixtures.IPS.navn,
            )
        }
        coVerify(exactly = 0) {
            sanityService.patchSanityTiltakstype(
                any(),
                TiltakstypeFixtures.Arbeidstrening.navn,
            )
        }
    }
})

private fun ProducerRecord<ByteArray, ByteArray?>.shouldBeTiltakstypeV3(topic: String, id: UUID): Boolean {
    val decoded = value()?.let { Json.decodeFromString<TiltakstypeV3Dto>(it.decodeToString()) }
    return topic() == topic &&
        key().decodeToString() == id.toString() &&
        decoded != null &&
        decoded.id == id
}
