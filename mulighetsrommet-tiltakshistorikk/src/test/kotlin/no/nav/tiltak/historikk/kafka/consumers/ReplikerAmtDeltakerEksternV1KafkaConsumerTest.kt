package no.nav.tiltak.historikk.kafka.consumers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.replikering.KometDeltakerReplikering
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReplikerAmtDeltakerEksternV1KafkaConsumerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val amtDeltakerEksternV1 = AmtDeltakerEksternV1Dto(
        id = TestFixtures.Deltaker.GruppeAmo.id,
        gjennomforingId = TestFixtures.Gjennomforing.GruppeAmo.id,
        personIdent = "10101010100",
        startDato = null,
        sluttDato = null,
        status = AmtDeltakerEksternV1Dto.StatusDto(
            type = DeltakerStatusType.VENTER_PA_OPPSTART,
            tekst = "Venter på oppstart",
            aarsak = AmtDeltakerEksternV1Dto.AarsakDto(null, null),
            opprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
        ),
        registrertTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
        endretTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
        kilde = AmtDeltakerEksternV1Dto.Kilde.KOMET,
        innhold = null,
        navVeileder = null,
        deltakelsesmengder = listOf(
            AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                deltakelsesprosent = 50f,
                dagerPerUke = 2.5f,
                gyldigFraDato = LocalDate.of(2022, 1, 1),
                opprettetTidspunkt = LocalDateTime.of(2022, 1, 1, 0, 0),
            ),
        ),
    )

    context("consume deltakere") {
        val db = TiltakshistorikkDatabase(database.db)

        val deltakerConsumer = ReplikerAmtDeltakerEksternV1KafkaConsumer(
            KometDeltakerReplikering(db),
            Clock.fixed(Instant.parse("2025-01-31T23:30:00Z"), ZoneId.of("Europe/Oslo")),
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
            val deltaker = amtDeltakerEksternV1.copy(
                status = amtDeltakerEksternV1.status.copy(
                    opprettetTidspunkt = LocalDateTime.parse("2025-01-01T10:00:00.123456789"),
                ),
                registrertTidspunkt = LocalDateTime.parse("2025-01-01T10:00:00.123456789"),
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00.987654789"),
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
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00.987654001"),
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

            deltakerConsumer.consume(amtDeltakerEksternV1.id, JsonNull)

            db.session {
                queries.kometDeltaker.get(amtDeltakerEksternV1.id).shouldBeNull()
            }
        }

        test("hopper over deltaker hvis innkommende endretTidspunkt er eldre enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val utdatertDeltaker = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.of(2021, 12, 31, 0, 0),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(utdatertDeltaker.id, Json.encodeToJsonElement(utdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(amtDeltakerEksternV1.id)?.startDato.shouldBeNull()
            }
        }

        test("oppdaterer deltaker hvis innkommende endretTidspunkt er nyere enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val oppdatertDeltaker = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.of(2022, 1, 2, 0, 0),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(oppdatertDeltaker.id, Json.encodeToJsonElement(oppdatertDeltaker))

            db.session {
                queries.kometDeltaker.get(amtDeltakerEksternV1.id)?.startDato shouldBe LocalDate.of(2025, 1, 1)
            }
        }

        test("sletter feilregistrert deltaker selv om meldingen er eldre enn lagret") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val feilregistrertDeltaker1 = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.of(2021, 12, 31, 0, 0),
                status = AmtDeltakerEksternV1Dto.StatusDto(
                    type = DeltakerStatusType.FEILREGISTRERT,
                    tekst = "Feilregistrert",
                    aarsak = AmtDeltakerEksternV1Dto.AarsakDto(null, null),
                    opprettetTidspunkt = LocalDateTime.of(2021, 12, 31, 0, 0),
                ),
            )
            deltakerConsumer.consume(feilregistrertDeltaker1.id, Json.encodeToJsonElement(feilregistrertDeltaker1))

            db.session {
                queries.kometDeltaker.get(feilregistrertDeltaker1.id).shouldBeNull()
            }
        }

        test("lagrer gjeldende deltakelsesmengde i norsk tid uavhengig av kilde") {
            val deltaker = amtDeltakerEksternV1.copy(
                kilde = AmtDeltakerEksternV1Dto.Kilde.ARENA,
                deltakelsesmengder = listOf(
                    AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                        deltakelsesprosent = 75f,
                        dagerPerUke = 3f,
                        gyldigFraDato = LocalDate.of(2025, 2, 1),
                        opprettetTidspunkt = LocalDateTime.of(2025, 1, 15, 12, 0),
                    ),
                    AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                        deltakelsesprosent = 90f,
                        dagerPerUke = 4f,
                        gyldigFraDato = LocalDate.of(2025, 3, 1),
                        opprettetTidspunkt = LocalDateTime.of(2025, 1, 15, 12, 0),
                    ),
                    amtDeltakerEksternV1.deltakelsesmengder.single(),
                ),
            )

            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(deltaker.id).shouldNotBeNull()
                lagret.prosentStilling shouldBe 75f
                lagret.dagerPerUke shouldBe 3f
            }
        }

        test("oppdaterer deltaker når tidsstempelet er mindre enn tre sekunder eldre") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(
                    TestFixtures.Deltaker.GruppeAmo.copy(
                        oppdatertTidspunkt = LocalDateTime.parse("2025-01-02T11:00:03"),
                    ),
                )
            }
            val deltaker = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00.000001"),
                startDato = LocalDate.of(2025, 1, 1),
            )

            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(deltaker.id).shouldNotBeNull()
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:00.000001")
                lagret.startDato shouldBe LocalDate.of(2025, 1, 1)
            }
        }

        test("oppdaterer deltaker når tidsstempelet er nøyaktig tre sekunder eldre") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(
                    TestFixtures.Deltaker.GruppeAmo.copy(
                        oppdatertTidspunkt = LocalDateTime.parse("2025-01-02T11:00:03"),
                    ),
                )
            }
            val deltaker = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00"),
                startDato = LocalDate.of(2025, 1, 1),
            )

            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(deltaker.id).shouldNotBeNull()
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:00")
                lagret.startDato shouldBe LocalDate.of(2025, 1, 1)
            }
        }

        test("beholder lagret deltaker når tidsstempelet er mer enn tre sekunder eldre") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(
                    TestFixtures.Deltaker.GruppeAmo.copy(
                        oppdatertTidspunkt = LocalDateTime.parse("2025-01-02T11:00:03.000001"),
                    ),
                )
            }
            val deltaker = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00"),
                startDato = LocalDate.of(2025, 1, 1),
                deltakelsesmengder = listOf(
                    amtDeltakerEksternV1.deltakelsesmengder.single().copy(
                        deltakelsesprosent = 75f,
                        dagerPerUke = 3f,
                    ),
                ),
            )

            deltakerConsumer.consume(deltaker.id, Json.encodeToJsonElement(deltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(deltaker.id).shouldNotBeNull()
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:03.000001")
                lagret.startDato.shouldBeNull()
                lagret.prosentStilling shouldBe 50f
                lagret.dagerPerUke shouldBe 2.5f
            }
        }

        test("korrigerer skjevt tidsstempel og deltakelsesmengde ved replay og godtar senere oppdateringer") {
            db.session {
                queries.kometDeltaker.upsertKometDeltaker(
                    TestFixtures.Deltaker.GruppeAmo.copy(
                        oppdatertTidspunkt = LocalDateTime.parse("2025-01-02T11:00:03"),
                    ),
                )
            }
            val korrigertDeltaker = amtDeltakerEksternV1.copy(
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:00"),
                deltakelsesmengder = listOf(
                    amtDeltakerEksternV1.deltakelsesmengder.single().copy(
                        deltakelsesprosent = 75f,
                        dagerPerUke = 3f,
                    ),
                ),
            )

            deltakerConsumer.consume(korrigertDeltaker.id, Json.encodeToJsonElement(korrigertDeltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(korrigertDeltaker.id).shouldNotBeNull()
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:00")
                lagret.prosentStilling shouldBe 75f
                lagret.dagerPerUke shouldBe 3f
            }

            deltakerConsumer.consume(korrigertDeltaker.id, Json.encodeToJsonElement(korrigertDeltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(korrigertDeltaker.id).shouldNotBeNull()
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:00")
                lagret.prosentStilling shouldBe 75f
                lagret.dagerPerUke shouldBe 3f
            }

            val nyereDeltaker = korrigertDeltaker.copy(
                endretTidspunkt = LocalDateTime.parse("2025-01-02T11:00:04"),
                startDato = LocalDate.of(2025, 1, 1),
            )
            deltakerConsumer.consume(nyereDeltaker.id, Json.encodeToJsonElement(nyereDeltaker))

            db.session {
                val lagret = queries.kometDeltaker.get(nyereDeltaker.id).shouldNotBeNull()
                lagret.oppdatertTidspunkt shouldBe LocalDateTime.parse("2025-01-02T11:00:04")
                lagret.startDato shouldBe LocalDate.of(2025, 1, 1)
                lagret.prosentStilling shouldBe 75f
                lagret.dagerPerUke shouldBe 3f
            }
        }
    }

    context("deltakelsesmengde") {
        val deltaker = amtDeltakerEksternV1.copy(
            startDato = LocalDate.of(2025, 1, 1),
            sluttDato = LocalDate.of(2025, 3, 31),
            deltakelsesmengder = listOf(
                AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                    deltakelsesprosent = 80f,
                    dagerPerUke = 4f,
                    gyldigFraDato = LocalDate.of(2025, 3, 1),
                    opprettetTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0),
                ),
                AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                    deltakelsesprosent = 50f,
                    dagerPerUke = 2.5f,
                    gyldigFraDato = LocalDate.of(2025, 1, 1),
                    opprettetTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0),
                ),
                AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                    deltakelsesprosent = 100f,
                    dagerPerUke = 5f,
                    gyldigFraDato = LocalDate.of(2025, 4, 1),
                    opprettetTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0),
                ),
            ),
        )

        test("bruker første mengde før deltakelsen starter") {
            val result = deltaker.toKometDeltaker(LocalDate.of(2024, 12, 1))

            result.prosentStilling shouldBe 50f
            result.dagerPerUke shouldBe 2.5f
        }

        test("bruker siste gjeldende mengde etter at deltakelsen er avsluttet") {
            val result = deltaker.toKometDeltaker(LocalDate.of(2025, 5, 1))

            result.prosentStilling shouldBe 80f
            result.dagerPerUke shouldBe 4f
        }

        test("beholder ukjent prosent og dager som null") {
            val result = deltaker.copy(
                deltakelsesmengder = listOf(
                    deltaker.deltakelsesmengder.first().copy(deltakelsesprosent = null, dagerPerUke = null),
                ),
            ).toKometDeltaker(LocalDate.of(2025, 3, 1))

            result.prosentStilling.shouldBeNull()
            result.dagerPerUke.shouldBeNull()
        }
    }
})
