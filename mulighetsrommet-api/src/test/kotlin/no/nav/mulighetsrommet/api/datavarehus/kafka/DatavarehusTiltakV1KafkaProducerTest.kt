package no.nav.mulighetsrommet.api.datavarehus.kafka

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltak
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingEksternV1Dto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.time.LocalDate
import java.util.*

class DatavarehusTiltakV1KafkaProducerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val config = DatavarehusTiltakV1KafkaProducer.Config(
        consumerId = "id",
        consumerGroupId = "group-id",
        consumerTopic = "consumer-topic",
        producerTopic = "producer-topic",
    )

    test("produserer tombstone-meldinger når tombstones blir konsumert") {
        val producerClient = mockk<KafkaProducerClient<String, String?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.db,
        )

        val key = UUID.randomUUID().toString()
        producer.consume(key, JsonNull)

        verify {
            producerClient.sendSync(
                match {
                    it.topic() == config.producerTopic && it.key() == key && it.value() == null
                },
            )
        }
    }

    test("publiserer datamodell tilpasset datavarehus som JSON når gjennomføring blir konsumert") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )
        domain.initialize(database.db)

        val producerClient = mockk<KafkaProducerClient<String, String?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.db,
        )

        val message = Json.encodeToJsonElement(
            TiltaksgjennomforingEksternV1Dto(
                id = AFT1.id,
                navn = AFT1.navn,
                tiltakstype = TiltaksgjennomforingEksternV1Dto.Tiltakstype(
                    id = TiltakstypeFixtures.AFT.id,
                    navn = TiltakstypeFixtures.AFT.navn,
                    arenaKode = TiltakstypeFixtures.AFT.arenaKode,
                    tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
                ),
                virksomhetsnummer = "123123123",
                startDato = LocalDate.now(),
                sluttDato = null,
                status = TiltaksgjennomforingStatus.GJENNOMFORES,
                oppstart = TiltaksgjennomforingOppstartstype.FELLES,
                tilgjengeligForArrangorFraOgMedDato = null,
                apentForPamelding = true,
            ),
        )

        producer.consume(AFT1.id.toString(), message)

        verify {
            producerClient.sendSync(
                match { record ->
                    record.topic() == config.producerTopic &&
                        record.key() == AFT1.id.toString() &&
                        record.value()?.let { Json.decodeFromString<DatavarehusTiltak>(it) } != null
                },
            )
        }
    }
})
