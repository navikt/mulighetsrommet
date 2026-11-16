package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.mulighetsrommet.api.contracts.tiltakstype.TiltakstypeV3Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeSystem
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.replikering.TiltakstypeReplikering
import java.time.Instant
import java.time.LocalDateTime

class ReplikerSisteTiltakstyperV3KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val db by lazy { TiltakshistorikkDatabase(database.db) }
    val consumer by lazy { ReplikerSisteTiltakstyperV3KafkaConsumer(TiltakstypeReplikering(db)) }
    val melding = TiltakstypeV3Dto(
        id = TestFixtures.Tiltakstype.arbeidstrening.tiltakstypeId,
        navn = "Arbeidstrening",
        tiltakskode = Tiltakskode.ARBEIDSTRENING,
        system = TiltakstypeSystem.ARBEIDSGIVERTILTAK,
        innsatsgrupper = emptySet(),
        arenaKode = "ARBTREN",
        deltakerRegistreringInnhold = null,
        opprettetTidspunkt = LocalDateTime.parse("2025-01-01T10:00:00.123456789"),
        oppdatertTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00.654321789"),
    )

    afterEach {
        database.truncateAll()
    }

    test("lagrer og oppdaterer tiltakstypens kildetidsstempler fra Kafka") {
        consumer.consume(melding.id, Json.encodeToJsonElement(melding))

        db.session {
            val lagret = queries.tiltakstype.getByTiltakskode(Tiltakskode.ARBEIDSTRENING)
            lagret.opprettetTidspunkt shouldBe Instant.parse("2025-01-01T09:00:00.123456Z")
            lagret.oppdatertTidspunkt shouldBe Instant.parse("2025-01-02T10:00:00.654321Z")
        }

        consumer.consume(
            melding.id,
            Json.encodeToJsonElement(
                melding.copy(
                    navn = "Nytt navn",
                    oppdatertTidspunkt = LocalDateTime.parse("2025-07-03T12:00:00.123456789"),
                ),
            ),
        )

        db.session {
            val lagret = queries.tiltakstype.getByTiltakskode(Tiltakskode.ARBEIDSTRENING)
            lagret.navn shouldBe "Nytt navn"
            lagret.opprettetTidspunkt shouldBe Instant.parse("2025-01-01T09:00:00.123456Z")
            lagret.oppdatertTidspunkt shouldBe Instant.parse("2025-07-03T10:00:00.123456Z")
        }
    }

    test("tiltakstyper uten kjente kildetidsstempler kan leses og fylles ut ved replikering") {
        db.session {
            queries.tiltakstype.upsert(
                TestFixtures.Tiltakstype.arbeidstrening.copy(
                    opprettetTidspunkt = null,
                    oppdatertTidspunkt = null,
                ),
            )
        }

        consumer.consume(melding.id, Json.encodeToJsonElement(melding))

        db.session {
            val lagret = queries.tiltakstype.getByTiltakskode(Tiltakskode.ARBEIDSTRENING)
            lagret.opprettetTidspunkt shouldBe Instant.parse("2025-01-01T09:00:00.123456Z")
            lagret.oppdatertTidspunkt shouldBe Instant.parse("2025-01-02T10:00:00.654321Z")
        }
    }
})
