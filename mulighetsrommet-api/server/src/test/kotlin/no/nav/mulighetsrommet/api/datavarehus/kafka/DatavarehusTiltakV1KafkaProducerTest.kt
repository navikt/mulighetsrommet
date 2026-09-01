package no.nav.mulighetsrommet.api.datavarehus.kafka

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.contracts.datavarehus.DatavarehusTiltakV1
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DatavarehusTiltakV1KafkaProducerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val config = DatavarehusTiltakV1KafkaProducer.Config(
        producerTopic = "producer-topic",
    )

    test("støtter tombstones") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.api,
        )

        val key = UUID.randomUUID()

        producer.consume(key, JsonNull)

        verify {
            producerClient.sendSync(
                match { record ->
                    record.topic() == config.producerTopic &&
                        record.key().decodeToString() == key.toString() &&
                        record.value() == null
                },
            )
        }
    }

    test("publiserer tombstone når gjennomføringen allerede er slettet fra databasen") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.api,
        )

        val key = UUID.randomUUID()

        val hendelse: TiltaksgjennomforingV2Dto = tiltaksgjennomforingHendelse(key)

        producer.consume(key, Json.encodeToJsonElement(hendelse))

        verify {
            producerClient.sendSync(
                match { record ->
                    record.topic() == config.producerTopic &&
                        record.key().decodeToString() == key.toString() &&
                        record.value() == null
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
        domain.initialize(database.api)

        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        val producer = DatavarehusTiltakV1KafkaProducer(
            config,
            producerClient,
            database.api,
        )

        var hendelse: TiltaksgjennomforingV2Dto = tiltaksgjennomforingHendelse(AFT1.id)

        producer.consume(AFT1.id, Json.encodeToJsonElement(hendelse))

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

private fun tiltaksgjennomforingHendelse(key: UUID): TiltaksgjennomforingV2Dto.Gruppe = TiltaksgjennomforingV2Dto.Gruppe(
    id = key,
    lopenummer = Tiltaksnummer("2025/1"),
    navn = "Gjennomføring som er slettet",
    tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
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
    pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
)
