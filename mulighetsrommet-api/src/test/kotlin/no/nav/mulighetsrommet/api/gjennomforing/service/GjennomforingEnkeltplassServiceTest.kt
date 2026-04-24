package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.model.Deltakelsesmengde
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GjennomforingEnkeltplassServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        ansatte = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
        gjennomforinger = listOf(GjennomforingFixtures.EnkelAmo),
    )

    beforeEach {
        domain.initialize(database.db)
    }

    afterEach {
        database.truncateAll()
    }

    fun createService(
        features: Map<Tiltakskode, Set<TiltakstypeFeature>> = mapOf(),
    ): GjennomforingEnkeltplassService {
        return GjennomforingEnkeltplassService(
            config = GjennomforingEnkeltplassService.Config(TEST_GJENNOMFORING_V2_TOPIC),
            db = database.db,
            personaliaService = mockk(),
            tiltakstyper = TiltakstypeService(TiltakstypeService.Config(features), database.db),
        )
    }

    val norskIdent = NorskIdent("12345678910")

    val migrert = mapOf(Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to setOf(TiltakstypeFeature.MIGRERT))

    val opprettetAv = NavAnsattFixture.DonaldDuck.navIdent
    val besluttetAv = NavAnsattFixture.MikkeMus.navIdent

    fun createEnkeltplass() = UpsertGjennomforingEnkeltplass(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        arrangorId = GjennomforingFixtures.EnkelAmo.arrangorId,
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 6, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        ansvarligEnhet = GjennomforingFixtures.EnkelAmo.ansvarligEnhet!!,
        prisbetingelser = null,
    )

    context("opprettelse gjennomføring") {
        val service = createService()

        test("oppretter totrinnskontroll når gjennomføringen opprettes av navident") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, opprettetAv).shouldBeRight()

            database.run {
                queries.totrinnskontroll.getOrError(gjennomforing.id, Totrinnskontroll.Type.OKONOMI).should {
                    it.behandletAv shouldBe opprettetAv
                    it.besluttelse.shouldBeNull()
                }
            }
        }

        test("oppretter ikke totrinnskontroll når gjennomføringen opprettes av Arena") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, Arena).shouldBeRight()

            database.run {
                queries.totrinnskontroll.get(gjennomforing.id, Totrinnskontroll.Type.OKONOMI).shouldBeNull()
            }
        }
    }
    context("godkjennOkonomi") {
        val service = createService()

        test("godkjenner økonomi og setter besluttelse til GODKJENT") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, opprettetAv).shouldBeRight()

            service.godkjennOkonomi(gjennomforing.id, besluttetAv).shouldBeRight()

            database.run {
                queries.totrinnskontroll.getOrError(gjennomforing.id, Totrinnskontroll.Type.OKONOMI).should {
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                    it.besluttetAv shouldBe besluttetAv
                }
            }
        }

        test("returnerer feil når behandletAv og besluttetAv er samme person") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, opprettetAv).shouldBeRight()

            service.godkjennOkonomi(gjennomforing.id, opprettetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Du kan ikke godkjenne økonomi for en gjennomføring du selv har opprettet"
        }

        test("kan godkjenne enkeltplass etter avvisning") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, opprettetAv).shouldBeRight()

            service.avvisOkonomi(gjennomforing.id, besluttetAv, forklaring = "Feil prisbetingelser").shouldBeRight()
            service.godkjennOkonomi(gjennomforing.id, besluttetAv).shouldBeRight()

            database.run {
                queries.totrinnskontroll.getOrError(gjennomforing.id, Totrinnskontroll.Type.OKONOMI).should {
                    it.besluttelse shouldBe Besluttelse.GODKJENT
                    it.forklaring shouldBe null
                }
            }
        }
    }

    context("avslaaOkonomi") {
        val service = createService()

        test("avslår økonomi og setter besluttelse til AVVIST") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, opprettetAv).shouldBeRight()

            service.avvisOkonomi(gjennomforing.id, besluttetAv, forklaring = "Feil").shouldBeRight()

            database.run {
                queries.totrinnskontroll.getOrError(gjennomforing.id, Totrinnskontroll.Type.OKONOMI).should {
                    it.besluttelse shouldBe Besluttelse.AVVIST
                    it.besluttetAv shouldBe besluttetAv
                    it.forklaring shouldBe "Feil"
                }
            }
        }

        test("returnerer feil når enkeltplass allerede er behandlet") {
            val gjennomforing = createEnkeltplass()
            service.create(gjennomforing, opprettetAv).shouldBeRight()

            service.godkjennOkonomi(gjennomforing.id, besluttetAv).shouldBeRight()

            service.godkjennOkonomi(gjennomforing.id, besluttetAv)
                .shouldBeLeft()
                .first().detail shouldBe "Kan ikke avvise enkeltplass som allerede er behandlet"

            service.avvisOkonomi(gjennomforing.id, besluttetAv, forklaring = "Angret")
                .shouldBeLeft()
                .first().detail shouldBe "Kan ikke avvise enkeltplass som allerede er behandlet"
        }
    }

    context("updateFromDeltaker") {
        val service = createService()

        test("lagrer hash av norsk ident i fritekstsøk for aktiv deltaker") {
            val deltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                status = DeltakerStatusType.DELTAR,
            )

            service.updateFromDeltaker(deltaker, norskIdent)

            database.run {
                queries.gjennomforing.getAll(search = "12345678910").items.shouldBeEmpty()
                queries.gjennomforing.getAll(
                    search = NorskIdentHasher.hashIfNorskIdent("12345678910"),
                ).items.shouldHaveSize(1).first().id shouldBe GjennomforingFixtures.EnkelAmo.id
            }
        }

        test("lagrer ikke norsk ident i fritekstsøk når deltaker er FEILREGISTRERT") {
            val deltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                status = DeltakerStatusType.FEILREGISTRERT,
            )

            service.updateFromDeltaker(deltaker, norskIdent)

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

                val deltaker = DeltakerFixtures.createDeltaker(
                    id = eksisterendeDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                service.updateFromDeltaker(deltaker, norskIdent)
            }

            test("kaster exception når gjennomføringen allerede har en annen deltaker") {
                val annenDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(annenDeltaker),
                ).initialize(database.db)

                val nyDeltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                shouldThrow<IllegalStateException> {
                    service.updateFromDeltaker(nyDeltaker, norskIdent)
                }.message shouldBe "Enkeltplass med id=${GjennomforingFixtures.EnkelAmo.id} har allerede en annen deltaker"
            }
        }

        context("når tiltakstype ikke er migrert") {
            test("oppdaterer ikke gjennomføring") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    startDato = LocalDate.of(2026, 1, 1),
                    sluttDato = LocalDate.of(2026, 6, 1),
                )

                val gjennomforing = service.updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
                gjennomforing.sluttDato shouldBe GjennomforingFixtures.EnkelAmo.sluttDato
                gjennomforing.status shouldBe GjennomforingFixtures.EnkelAmo.status
            }

            test("publiserer ikke til kafka") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                service.updateFromDeltaker(deltaker, norskIdent)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC)).shouldBeEmpty()
                }
            }
        }

        context("når tiltakstype er migrert") {
            test("oppdaterer gjennomføring med datoer og status fra deltaker") {
                val startDato = LocalDate.of(2025, 3, 1)
                val sluttDato = LocalDate.of(2025, 6, 1)

                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    startDato = startDato,
                    sluttDato = sluttDato,
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("bruker gjennomføringens startdato når deltaker ikke har startdato") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.VENTER_PA_OPPSTART,
                    startDato = null,
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe GjennomforingFixtures.EnkelAmo.startDato
            }

            test("setter status AVBRUTT når deltaker er FEILREGISTRERT") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FEILREGISTRERT,
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.status shouldBe GjennomforingStatusType.AVBRUTT
            }

            test("setter status AVSLUTTET når deltaker er FULLFORT") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.FULLFORT,
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.status shouldBe GjennomforingStatusType.AVSLUTTET
            }

            test("bruker deltakelsesprosent fra siste deltakelsesmengde") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                ).copy(
                    deltakelsesmengder = listOf(
                        Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2025, 1, 1),
                            deltakelsesprosent = 50.0,
                        ),
                        Deltakelsesmengde(
                            gyldigFra = LocalDate.of(2025, 3, 1),
                            deltakelsesprosent = 75.0,
                        ),
                    ),
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.deltidsprosent shouldBe 75.0
            }

            test("bruker 100 prosent som standardverdi når deltaker ikke har deltakelsesmengder") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                ).copy(deltakelsesmengder = emptyList())

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.deltidsprosent shouldBe 100.0
            }

            test("persisterer oppdatert gjennomføring i databasen") {
                val startDato = LocalDate.of(2025, 3, 1)
                val sluttDato = LocalDate.of(2025, 6, 1)

                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    startDato = startDato,
                    sluttDato = sluttDato,
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.startDato shouldBe startDato
                gjennomforing.sluttDato shouldBe sluttDato
                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("publiserer gjennomføring til kafka") {
                val deltaker = DeltakerFixtures.createDeltaker(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                )

                createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                database.run {
                    queries.kafkaProducerRecord.getRecords(10, listOf(TEST_GJENNOMFORING_V2_TOPIC))
                        .shouldHaveSize(1).first().key.decodeToString()
                        .shouldBe(GjennomforingFixtures.EnkelAmo.id.toString())
                }
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

                val service = createService(migrert)

                val deltakerAvbrutt = DeltakerFixtures.createDeltaker(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.AVBRUTT,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                service.updateFromDeltaker(
                    deltakerAvbrutt,
                    norskIdent,
                ).status shouldBe GjennomforingStatusType.AVBRUTT

                val deltakerDeltar = DeltakerFixtures.createDeltaker(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.DELTAR,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                service.updateFromDeltaker(
                    deltakerDeltar,
                    norskIdent,
                ).status shouldBe GjennomforingStatusType.GJENNOMFORES
            }

            test("hopper over når deltaker-eventet er eldre enn lagret") {
                val lagretDeltaker = DeltakerFixtures.createDeltakerDbo(
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    endretTidspunkt = nyereEndretTidspunkt,
                )
                MulighetsrommetTestDomain(
                    deltakere = listOf(lagretDeltaker),
                ).initialize(database.db)

                val deltaker = DeltakerFixtures.createDeltaker(
                    id = lagretDeltaker.id,
                    gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
                    status = DeltakerStatusType.AVBRUTT,
                    endretTidspunkt = tidligereEndretTidspunkt,
                )

                val gjennomforing = createService(migrert).updateFromDeltaker(deltaker, norskIdent)

                gjennomforing.status shouldBe GjennomforingStatusType.GJENNOMFORES
            }
        }
    }
})
