package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.time.LocalDateTime

class GjennomforingEnkeltplassServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createService(erMigrert: Boolean = false): GjennomforingEnkeltplassService {
        val features = if (erMigrert) {
            mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))
        } else {
            emptyMap()
        }
        return GjennomforingEnkeltplassService(
            config = GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            db = database.db,
            tiltakstyper = TiltakstypeService(TiltakstypeService.Config(features), database.db),
            deltakerClient = mockk(),
        )
    }

    context("upsertFromDeltaker") {
        test("lagrer hash av norsk ident i fritekstsøk for aktiv deltaker") {
            val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                status = DeltakerStatusType.DELTAR,
                personIdent = "12345678910",
            )

            createService().upsertFromDeltaker(deltaker)

            database.run {
                queries.gjennomforing.getAll(search = "12345678910").items.shouldBeEmpty()
                queries.gjennomforing.getAll(
                    search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
                ).items.shouldHaveSize(1).first().id shouldBe GjennomforingFixtures.EnkelAmo.id
            }
        }

        test("lagrer ikke norsk ident i fritekstsøk når deltaker er FEILREGISTRERT") {
            val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                status = DeltakerStatusType.FEILREGISTRERT,
                personIdent = "12345678910",
            )

            createService().upsertFromDeltaker(deltaker)

            database.run {
                queries.gjennomforing.getAll(
                    search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
                ).items.shouldBeEmpty()
            }
        }

        context("validering av én deltaker per enkeltplass") {
            test("godtar deltakeren som allerede er tilknyttet gjennomføringen") {
                val eksisterendeDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(eksisterendeDeltaker),
                ).initialize(database.db)

                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    id = eksisterendeDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                )

                createService().upsertFromDeltaker(deltaker)
            }

            test("kaster exception når gjennomføringen allerede har en annen deltaker") {
                val annenDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(annenDeltaker),
                ).initialize(database.db)

                val nyDeltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                )

                shouldThrow<IllegalStateException> {
                    createService().upsertFromDeltaker(nyDeltaker)
                }.message shouldBe "Enkeltplass med id=${GjennomforingFixtures.EnkelAmo.id} har allerede en annen deltaker"
            }
        }

        context("relast av deltaker") {
            val tidligereEndretTidspunkt = LocalDateTime.of(2025, 1, 1, 12, 0, 0)
            val nyereEndretTidspunkt = LocalDateTime.of(2025, 6, 1, 12, 0, 0)

            test("prosesserer når deltaker-eventet er samme eller nyere enn lagret") {
                val lagretDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = tidligereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.db)

                val service = createService(erMigrert = true)

                val deltakerAvbrutt = DeltakerFixtures.createAmtDeltakerDto(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.AVBRUTT,
                    personIdent = "12345678910",
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                service.upsertFromDeltaker(deltakerAvbrutt).status shouldBe GjennomforingStatusType.AVBRUTT

                val deltakerDeltar = DeltakerFixtures.createAmtDeltakerDto(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                service.upsertFromDeltaker(deltakerDeltar).status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("hopper over når deltaker-eventet er eldre enn lagret") {
                val lagretDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.db)

                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.AVBRUTT,
                    personIdent = "12345678910",
                    endretTidspunkt = tidligereEndretTidspunkt,
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }
        }

        context("når tiltakstype er migrert") {
            test("oppdaterer gjennomføring med datoer og status fra deltaker") {
                val startDato = LocalDate.of(2025, 3, 1)
                val sluttDato = LocalDate.of(2025, 6, 1)

                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                    startDato = startDato,
                    sluttDato = sluttDato,
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("bruker gjennomføringens startdato når deltaker ikke har startdato") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.VENTER_PA_OPPSTART,
                    personIdent = "12345678910",
                    startDato = null,
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
            }

            test("setter status AVBRUTT når deltaker er FEILREGISTRERT") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FEILREGISTRERT,
                    personIdent = "12345678910",
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.status shouldBe GjennomforingStatusType.AVBRUTT
            }

            test("setter status AVSLUTTET når deltaker er FULLFORT") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FULLFORT,
                    personIdent = "12345678910",
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.status shouldBe GjennomforingStatusType.AVSLUTTET
            }

            test("bruker deltakelsesprosent fra siste deltakelsesmengde") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                ).copy(
                    deltakelsesmengder = listOf(
                        AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                            deltakelsesprosent = 50f,
                            dagerPerUke = null,
                            gyldigFraDato = LocalDate.of(2025, 1, 1),
                            opprettetTidspunkt = LocalDateTime.now(),
                        ),
                        AmtDeltakerEksternV1Dto.DeltakelsesmengdeDto(
                            deltakelsesprosent = 75f,
                            dagerPerUke = null,
                            gyldigFraDato = LocalDate.of(2025, 3, 1),
                            opprettetTidspunkt = LocalDateTime.now(),
                        ),
                    ),
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.deltidsprosent shouldBe 75.0
            }

            test("bruker 100 prosent som standardverdi når deltaker ikke har deltakelsesmengder") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                ).copy(deltakelsesmengder = emptyList())

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.deltidsprosent shouldBe 100.0
            }

            test("persisterer oppdatert gjennomføring i databasen") {
                val startDato = LocalDate.of(2025, 3, 1)
                val sluttDato = LocalDate.of(2025, 6, 1)

                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                    startDato = startDato,
                    sluttDato = sluttDato,
                )

                val gjennomforing = createService(erMigrert = true).upsertFromDeltaker(deltaker)

                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("publiserer gjennomføring til kafka") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                )

                createService(erMigrert = true).upsertFromDeltaker(deltaker)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC))
                        .shouldHaveSize(1).first().key.decodeToString()
                        .shouldBe(GjennomforingFixtures.EnkelAmo.id.toString())
                }
            }
        }

        context("når tiltakstype ikke er migrert") {
            test("oppdaterer ikke gjennomføring") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                    startDato = LocalDate.of(2026, 1, 1),
                    sluttDato = LocalDate.of(2026, 6, 1),
                )

                val gjennomforing = createService(erMigrert = false).upsertFromDeltaker(deltaker)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
                gjennomforing.sluttDato shouldBe GjennomforingFixtures.EnkelAmo.sluttDato
                gjennomforing.status shouldBe GjennomforingFixtures.EnkelAmo.status
            }

            test("publiserer ikke til kafka") {
                val deltaker = DeltakerFixtures.createAmtDeltakerDto(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    personIdent = "12345678910",
                )

                createService(erMigrert = false).upsertFromDeltaker(deltaker)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldBeEmpty()
                }
            }
        }
    }
})
