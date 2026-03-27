package no.nav.mulighetsrommet.api.gjennomforing.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.api.gjennomforing.service.TEST_GJENNOMFORING_V2_TOPIC
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDateTime

class ReplikerDeltakerEnkeltplassKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    fun createConsumer(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
        service: GjennomforingEnkeltplassService = GjennomforingEnkeltplassService(
            GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            database.db,
            TiltakstypeService(TiltakstypeService.Config(features), database.db),
            mockk(),
        ),
    ): ReplikerDeltakerEnkeltplassKafkaConsumer {
        return ReplikerDeltakerEnkeltplassKafkaConsumer(
            db = database.db,
            service = service,
        )
    }

    test("oppdaterer ikke gjennomføring av typen avtale") {
        MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        ).initialize(database.db)

        val deltaker = DeltakerFixtures.createAmtDeltakerDto(
            gjennomforingId = AFT1.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
        )

        val features = mapOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to setOf(TiltakstypeFeature.MIGRERT))
        createConsumer(features).consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingOrError(AFT1.id).status shouldBe GjennomforingStatusType.GJENNOMFORES
        }
    }

    test("oppdaterer ikke gjennomføring når tiltakstype enda ikke er migrert") {
        MulighetsrommetTestDomain(
            gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.GJENNOMFORES)),
        ).initialize(database.db)

        val deltaker = DeltakerFixtures.createAmtDeltakerDto(
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
        )

        createConsumer().consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingOrError(EnkelAmo.id).status shouldBe GjennomforingStatusType.GJENNOMFORES
        }
    }

    test("oppdaterer gjennomføring når tiltakstypen er migrert") {
        MulighetsrommetTestDomain(
            gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.GJENNOMFORES)),
        ).initialize(database.db)

        val deltaker = DeltakerFixtures.createAmtDeltakerDto(
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
        )

        val features = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))
        createConsumer(features).consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingOrError(EnkelAmo.id).status shouldBe GjennomforingStatusType.AVSLUTTET
        }
    }

    test("oppdaterer ikke gjennomføring når hendelse er før endretTidspunkt på lagret deltaker") {
        val tidspunktMs = LocalDateTime.of(2025, 1, 1, 12, 0, 0, 123_456_000)
        val lagretDeltaker = DeltakerFixtures.createDeltakerDbo(
            gjennomforingId = EnkelAmo.id,
            endretTidspunkt = tidspunktMs,
        )
        MulighetsrommetTestDomain(
            gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.GJENNOMFORES)),
            deltakere = listOf(lagretDeltaker),
        ).initialize(database.db)

        val features = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))
        val consumer = createConsumer(features)

        val deltaker = DeltakerFixtures.createAmtDeltakerDto(
            id = lagretDeltaker.id,
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
            endretTidspunkt = tidspunktMs.withNano(123_455_999),
        )
        consumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingOrError(EnkelAmo.id).status shouldBe GjennomforingStatusType.GJENNOMFORES
        }

        consumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker.copy(endretTidspunkt = tidspunktMs)))

        database.run {
            queries.gjennomforing.getGjennomforingOrError(EnkelAmo.id).status shouldBe GjennomforingStatusType.AVSLUTTET
        }
    }
})
