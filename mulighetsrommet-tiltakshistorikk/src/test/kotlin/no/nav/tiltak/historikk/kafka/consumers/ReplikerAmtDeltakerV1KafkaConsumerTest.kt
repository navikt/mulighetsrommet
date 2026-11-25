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

        val amtDeltakerV1 = AmtDeltakerV1Dto(
            id = TestFixtures.Deltaker.GruppeAmo.id,
            gjennomforingId = TestFixtures.Gjennomforing.GruppeAmo.id,
            personIdent = "10101010100",
            startDato = null,
            sluttDato = null,
            status = AmtDeltakerV1Dto.DeltakerStatusDto(
                type = DeltakerStatusType.VENTER_PA_OPPSTART,
                aarsak = null,
                opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
            ),
            registrertDato = LocalDateTime.of(2022, 1, 1, 0, 0),
            endretDato = LocalDateTime.of(2022, 1, 1, 0, 0),
            dagerPerUke = 2.5f,
            prosentStilling = 50f,
        )

        beforeEach {
            db.session {
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.GruppeAmo)
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arrangor)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.GruppeAmo)
            }
        }

        afterEach {
            database.truncateAll()
        }

        test("normaliserer deltakerens tidsstempler før lagring og sammenligning") {
            val deltaker = amtDeltakerV1.copy(
                status = amtDeltakerV1.status.copy(
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
                lagret.dagerPerUke shouldBe 2.5f
                lagret.prosentStilling shouldBe 50f
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
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            deltakerConsumer.consume(amtDeltakerV1.id, JsonNull)

            db.session {
                queries.kometDeltaker.get(amtDeltakerV1.id).shouldBeNull()
            }
        }

        test("hopper over deltaker hvis innkommende endretTidspunkt er eldre enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val utdatertDeltaker = amtDeltakerV1.copy(
                endretDato = LocalDateTime.parse("2021-12-31T00:00:00"),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(utdatertDeltaker.id, Json.encodeToJsonElement(utdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(amtDeltakerV1.id)?.startDato.shouldBeNull()
            }
        }

        test("oppdaterer deltaker hvis innkommende endretTidspunkt er nyere enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val oppdatertDeltaker = amtDeltakerV1.copy(
                endretDato = LocalDateTime.parse("2022-01-02T00:00:00"),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(oppdatertDeltaker.id, Json.encodeToJsonElement(oppdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(amtDeltakerV1.id)?.startDato shouldBe LocalDate.of(2025, 1, 1)
            }
        }

        test("sletter feilregistrert deltaker selv om meldingen er eldre enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val feilregistrertDeltaker1 = amtDeltakerV1.copy(
                endretDato = LocalDateTime.parse("2021-12-31T00:00:00"),
                status = AmtDeltakerV1Dto.DeltakerStatusDto(
                    type = DeltakerStatusType.FEILREGISTRERT,
                    aarsak = null,
                    opprettetDato = LocalDateTime.parse("2022-01-01T00:00:00"),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            db.session {
                queries.kometDeltaker.get(feilregistrertDeltaker1.id).shouldBeNull()
            }
        }
    }
})
