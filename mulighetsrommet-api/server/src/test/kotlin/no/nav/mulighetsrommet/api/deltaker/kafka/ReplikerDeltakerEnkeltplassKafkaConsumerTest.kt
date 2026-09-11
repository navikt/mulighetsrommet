package no.nav.mulighetsrommet.api.deltaker.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDateTime

class ReplikerDeltakerEnkeltplassKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    fun createConsumer(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ): ReplikerDeltakerEnkeltplassKafkaConsumer {
        val service = GjennomforingEnkeltplassService(
            database.api,
            mockk(),
            TiltakstypeService(TiltakstypeService.Config(features), database.admin),
        )
        return ReplikerDeltakerEnkeltplassKafkaConsumer(
            db = database.api,
            service = service,
        )
    }

    test("oppdaterer ikke gjennomføring av typen avtale") {
        MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.AFT),
            gjennomforinger = listOf(AFT1),
        ).initialize(database.api)

        val deltaker = AmtDeltakerEksternV1DtoFixtures.createAmtDeltakerDto(
            gjennomforingId = AFT1.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
        )

        val features = mapOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to setOf(TiltakstypeFeature.MIGRERT))
        createConsumer(features).consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingAvtaleOrError(AFT1.id).status shouldBe GjennomforingAvtaleStatus.Gjennomfores
        }
    }

    test("oppdaterer ikke gjennomføring når tiltakstype enda ikke er migrert") {
        MulighetsrommetTestDomain(
            gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.ENKELTPLASS_DELTAR)),
        ).initialize(database.api)

        val deltaker = AmtDeltakerEksternV1DtoFixtures.createAmtDeltakerDto(
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
        )

        createConsumer().consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingEnkeltplassOrError(EnkelAmo.id).status shouldBe GjennomforingEnkeltplassStatus.Deltar
        }
    }

    test("oppdaterer gjennomføring når tiltakstypen er migrert") {
        MulighetsrommetTestDomain(
            gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.ENKELTPLASS_DELTAR)),
        ).initialize(database.api)

        val deltaker = AmtDeltakerEksternV1DtoFixtures.createAmtDeltakerDto(
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
        )

        val features = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))
        createConsumer(features).consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingEnkeltplassOrError(EnkelAmo.id).status shouldBe GjennomforingEnkeltplassStatus.Fullfort
        }
    }

    test("oppdaterer ikke gjennomføring når hendelse er før endretTidspunkt på lagret deltaker") {
        val tidspunktMs = LocalDateTime.of(2025, 1, 1, 12, 0, 0, 123_456_000)
        val lagretDeltaker = DeltakerFixtures.createDeltaker(
            gjennomforingId = EnkelAmo.id,
            endretTidspunkt = tidspunktMs.tilNorskInstant(),
        )
        MulighetsrommetTestDomain(
            gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.ENKELTPLASS_DELTAR)),
            deltakere = listOf(lagretDeltaker),
        ).initialize(database.api)

        val features = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))
        val consumer = createConsumer(features)

        val deltaker = AmtDeltakerEksternV1DtoFixtures.createAmtDeltakerDto(
            id = lagretDeltaker.id,
            gjennomforingId = EnkelAmo.id,
            status = DeltakerStatusType.FULLFORT,
            personIdent = "12345678910",
            endretTidspunkt = tidspunktMs.withNano(123_455_999),
        )
        consumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

        database.run {
            queries.gjennomforing.getGjennomforingEnkeltplassOrError(EnkelAmo.id).status shouldBe GjennomforingEnkeltplassStatus.Deltar
        }

        consumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker.copy(endretTidspunkt = tidspunktMs)))

        database.run {
            queries.gjennomforing.getGjennomforingEnkeltplassOrError(EnkelAmo.id).status shouldBe GjennomforingEnkeltplassStatus.Fullfort
        }
    }
})
