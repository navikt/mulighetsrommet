package no.nav.mulighetsrommet.api.datavarehus.kafka

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.datavarehus.model.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DatavarehusTiltakV1KafkaProducerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val config = DatavarehusTiltakV1KafkaProducer.Config(
        producerTopic = "producer-topic",
    )

    test("støtter ikke tombstones") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.db,
        )

        val key = UUID.randomUUID().toString()

        assertThrows<UnsupportedOperationException> {
            producer.consume(key, JsonNull)
        }
    }

    test("publiserer datamodell tilpasset datavarehus som JSON når gjennomføring blir konsumert") {
        val domain = MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.AFT),
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        )
        domain.initialize(database.db)

        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.db,
        )

        var gjennomforing: TiltaksgjennomforingV2Dto = TiltaksgjennomforingV2Dto.Gruppe(
            id = AFT1.id,
            navn = AFT1.navn,
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode!!,
            arrangor = TiltaksgjennomforingV2Dto.Arrangor(
                organisasjonsnummer = Organisasjonsnummer("123123123"),
            ),
            startDato = LocalDate.now(),
            sluttDato = null,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.FELLES,
            tilgjengeligForArrangorFraOgMedDato = null,
            apentForPamelding = true,
            antallPlasser = 10,
            deltidsprosent = 100.0,
            opprettetTidspunkt = Instant.now(),
            oppdatertTidspunkt = Instant.now(),
            oppmoteSted = null,
        )

        producer.consume(AFT1.id.toString(), Json.encodeToJsonElement(gjennomforing))

        verify {
            producerClient.sendSync(
                match { record ->
                    record.topic() == config.producerTopic &&
                        record.key().decodeToString() == AFT1.id.toString() &&
                        record.value()?.let { Json.decodeFromString<DatavarehusTiltakV1>(it.decodeToString()) } != null
                },
            )
        }
    }
})
