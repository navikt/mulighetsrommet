package no.nav.mulighetsrommet.api.deltaker.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingEnkeltplassService
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.AFT1
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures.EnkelAmo
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class ReplikerDeltakerEnkeltplassKafkaConsumerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    afterEach {
        database.truncateAll()
    }

    fun createConsumer(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ): ReplikerDeltakerEnkeltplassKafkaConsumer {
        val service = GjennomforingEnkeltplassService(
            database.admin,
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

    test("oppdaterer ikke gjennomføring når hendelse er mer enn tre sekunder eldre enn lagret deltaker") {
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
            endretTidspunkt = LocalDateTime.parse("2025-01-01T11:59:57.123455"),
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

    listOf(
        "2025-01-01T12:00:00.000001" to "mindre enn tre sekunder",
        "2025-01-01T12:00:00" to "nøyaktig tre sekunder",
    ).forEach { (endretTidspunkt, skjevhet) ->
        test("oppdaterer gjennomføring når deltakerens tidsstempel er $skjevhet eldre") {
            val lagretDeltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = EnkelAmo.id,
                startDato = LocalDate.of(2025, 1, 1),
                sluttDato = LocalDate.of(2025, 3, 1),
                endretTidspunkt = Instant.parse("2025-01-01T11:00:03Z"),
            )
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(EnkelAmo.copy(status = GjennomforingStatusType.ENKELTPLASS_DELTAR)),
                deltakere = listOf(lagretDeltaker),
            ).initialize(database.api)
            val deltaker = AmtDeltakerEksternV1DtoFixtures.createAmtDeltakerDto(
                id = lagretDeltaker.id,
                gjennomforingId = EnkelAmo.id,
                status = DeltakerStatusType.FULLFORT,
                personIdent = "12345678910",
                endretTidspunkt = LocalDateTime.parse(endretTidspunkt),
            )
            val features = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))
            val consumer = createConsumer(features)

            consumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

            database.run {
                queries.gjennomforing.getGjennomforingEnkeltplassOrError(EnkelAmo.id).status shouldBe GjennomforingEnkeltplassStatus.Fullfort
            }
        }
    }
})
