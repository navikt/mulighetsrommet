package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.arenaadapter.ArenaAdapterClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.ArenaMigreringTiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.ArenaTiltaksgjennomforingDto
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto.Gruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ArenaMigreringGjennomforingKafkaProducerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("migrerte gjennomføringer") {
        val producerClient = mockk<KafkaProducerClient<ByteArray, ByteArray?>>(relaxed = true)

        MulighetsrommetTestDomain(
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
        ) {
            queries.gjennomforing.setNavEnheter(GjennomforingFixtures.Oppfolging1.id, setOf(NavEnhetNummer("0400")))
        }.initialize(database.db)

        val gjennomforing = Gruppe(
            id = GjennomforingFixtures.Oppfolging1.id,
            opprettetTidspunkt = Instant.now(),
            oppdatertTidspunkt = Instant.now(),
            tiltakskode = Tiltakskode.OPPFOLGING,
            arrangor = TiltaksgjennomforingV2Dto.Arrangor(ArrangorFixtures.underenhet1.organisasjonsnummer),
            navn = "Gjennomføring",
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = null,
            status = GjennomforingStatusType.GJENNOMFORES,
            oppstart = GjennomforingOppstartstype.LOPENDE,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            tilgjengeligForArrangorFraOgMedDato = null,
            apentForPamelding = true,
            antallPlasser = 10,
            deltidsprosent = 100.0,
            oppmoteSted = null,
        )

        val migrert = TiltakstypeService.Config(
            mapOf(
                Tiltakskode.OPPFOLGING to setOf(TiltakstypeFeature.MIGRERT),
            ),
        )

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

            val tiltakstyper = TiltakstypeService(db = database.db)

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

            verify(exactly = 0) { producerClient.sendSync(any()) }
        }

        test("skal publisere gjennomføringer til tiltaksgjennomføringer når tiltakstype er migrert") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns null

            val tiltakstyper = TiltakstypeService(migrert, database.db)

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

            verify(exactly = 1) {
                producerClient.sendSync(
                    match { it.shouldBeArenaMigreringTiltaksgjennomforingDto(gjennomforing.id, null) },
                )
            }
        }

        test("skal inkludere eksisterende arenaId når gjennomføring allerede eksisterer i Arena") {
            val arenaAdapterClient = mockk<ArenaAdapterClient>()
            coEvery { arenaAdapterClient.hentArenadata(gjennomforing.id) } returns ArenaTiltaksgjennomforingDto(
                arenaId = 123,
                status = "AVSLU",
            )

            val tiltakstyper = TiltakstypeService(migrert, database.db)

            val consumer = createConsumer(tiltakstyper, arenaAdapterClient)
            consumeGjennomforing(consumer, gjennomforing)

            verify(exactly = 1) {
                producerClient.sendSync(
                    match { it.shouldBeArenaMigreringTiltaksgjennomforingDto(gjennomforing.id, 123) },
                )
            }
        }
    }
})

private suspend fun consumeGjennomforing(
    consumer: ArenaMigreringGjennomforingKafkaProducer,
    gjennomforing: TiltaksgjennomforingV2Dto,
) {
    consumer.consume(gjennomforing.id.toString(), Json.encodeToJsonElement(gjennomforing))
}

private fun ProducerRecord<ByteArray, ByteArray?>.shouldBeArenaMigreringTiltaksgjennomforingDto(
    id: UUID,
    arenaId: Int?,
): Boolean {
    val decoded = value()?.let { Json.decodeFromString<ArenaMigreringTiltaksgjennomforingDto>(it.decodeToString()) }
    return checkEquals(key().decodeToString(), id.toString()) &&
        decoded != null &&
        checkEquals(decoded.arenaId, arenaId)
}

private fun <T> checkEquals(a: T, b: T): Boolean {
    check(a == b) {
        "Expected '$a' to be equal to '$b'"
    }

    return true
}
