package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.replikering.KometDeltakerReplikering
import java.time.LocalDate
import java.time.LocalDateTime

class ReplikerAmtDeltakerV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("consume deltakere") {
        val db = TiltakshistorikkDatabase(database.db)

        val deltakerConsumer = ReplikerAmtDeltakerV1KafkaConsumer(KometDeltakerReplikering(db))

        val amtDeltaker1 = TestFixtures.Deltaker.gruppeAmo

        beforeEach {
            db.session {
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.gruppeAmo)
                queries.virksomhet.upsert(TestFixtures.Virksomhet.arrangor)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.gruppeAmo.toGjennomforing())
            }
        }

        afterEach {
            database.truncateAll()
        }

        test("normaliserer deltakerens tidsstempler før lagring og sammenligning") {
            val deltaker = amtDeltaker1.copy(
                status = amtDeltaker1.status.copy(
                    opprettetDato = LocalDateTime.parse("2025-01-01T10:00:00.123456789"),
                ),
                registrertDato = LocalDateTime.parse("2025-01-01T10:00:00.123456789"),
                endretDato = LocalDateTime.parse("2025-01-02T11:00:00.987654789"),
            )
            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(deltaker.id).shouldNotBeNull()
                lagret.statusOpprettetTidspunkt shouldBe LocalDateTime.parse("2025-01-01T10:00:00.123456")
                lagret.opprettetTidspunkt shouldBe LocalDateTime.parse("2025-01-01T10:00:00.123456")
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:00.987654")
            }

            val oppdatertDeltaker = deltaker.copy(
                startDato = LocalDate.of(2025, 2, 1),
                endretDato = LocalDateTime.parse("2025-01-02T11:00:00.987654001"),
            )
            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(oppdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(deltaker.id)?.startDato shouldBe LocalDate.of(2025, 2, 1)
            }
        }

        test("delete deltakere for tombstone messages") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(amtDeltaker1.toKometDeltaker())
            }

            deltakerConsumer.consume(amtDeltaker1.id, JsonNull)

            db.session {
                queries.kometDeltaker.get(amtDeltaker1.id).shouldBeNull()
            }
        }

        test("hopper over deltaker hvis innkommende endretTidspunkt er eldre enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(amtDeltaker1.toKometDeltaker())
            }

            val utdatertDeltaker = amtDeltaker1.copy(
                endretDato = amtDeltaker1.endretDato.minusDays(1),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(utdatertDeltaker.id, Json.encodeToJsonElement(utdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(amtDeltaker1.id)?.startDato shouldBe amtDeltaker1.startDato
            }
        }

        test("oppdaterer deltaker hvis innkommende endretTidspunkt er nyere enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(amtDeltaker1.toKometDeltaker())
            }

            val oppdatertDeltaker = amtDeltaker1.copy(
                endretDato = amtDeltaker1.endretDato.plusDays(1),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(oppdatertDeltaker.id, Json.encodeToJsonElement(oppdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(amtDeltaker1.id)?.startDato shouldBe oppdatertDeltaker.startDato
            }
        }

        test("sletter feilregistrert deltaker selv om meldingen er eldre enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(amtDeltaker1.toKometDeltaker())
            }

            val feilregistrertDeltaker1 = amtDeltaker1.copy(
                endretDato = amtDeltaker1.endretDato.minusDays(1),
                status = AmtDeltakerV1Dto.DeltakerStatusDto(
                    type = DeltakerStatusType.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.now(),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            db.session {
                queries.kometDeltaker.get(feilregistrertDeltaker1.id).shouldBeNull()
            }
        }
    }
})
