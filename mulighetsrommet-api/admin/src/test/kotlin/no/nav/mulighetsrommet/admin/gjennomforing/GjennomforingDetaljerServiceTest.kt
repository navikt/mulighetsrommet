package no.nav.mulighetsrommet.admin.gjennomforing

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.admin.testing.GjennomforingFixtures
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.Enkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.api.domain.personalia.Gradering
import no.nav.mulighetsrommet.api.domain.personalia.Personalia
import no.nav.mulighetsrommet.api.domain.testing.fixture.DeltakerFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavAnsattFixture
import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.time.LocalDate
import java.util.UUID

class GjennomforingDetaljerServiceTest : FunSpec({
    val norskIdent = NorskIdent("12345678910")

    fun createService(
        db: TestAdminDatabase = TestAdminDatabase(),
        personaliaPort: PersonaliaPort = mockk(),
        enkeltplassPort: EnkeltplassPort = mockk(),
        auditLog: GjennomforingAuditLog = mockk(relaxed = true),
    ) = GjennomforingDetaljerService(
        db = db,
        tiltakstypeService = TiltakstypeService(db = db),
        navAnsattService = mockk(),
        personaliaPort = personaliaPort,
        enkeltplassPort = enkeltplassPort,
        auditLog = auditLog,
    )

    context("tilgang til økonomihandlinger") {
        val kostnadssted = NavEnhetNummer("0400")

        test("enkeltplass-saksbehandler får ikke tilgang til avtalehandlinger") {
            val ansatt = NavAnsattFixture.DonaldDuck.medRoller(
                setOf(NavAnsattRolle.generell(Rolle.OKONOMI_SAKSBEHANDLER_ENKELTPLASS)),
            )

            GjennomforingDetaljerService.tilgangTilHandling(
                ansatt,
                GjennomforingHandling.OPPRETT_UTBETALING,
                GjennomforingType.ENKELTPLASS,
            ) shouldBe true
            GjennomforingDetaljerService.tilgangTilHandling(
                ansatt,
                GjennomforingHandling.OPPRETT_UTBETALING,
                GjennomforingType.AVTALE,
            ) shouldBe false
        }

        test("enkeltplass-beslutter må ha rolle ved ansvarlig enhet") {
            val ansatt = NavAnsattFixture.MikkeMus.medRoller(
                setOf(NavAnsattRolle.kontorspesifikk(Rolle.OKONOMI_BESLUTTER_ENKELTPLASS, setOf(kostnadssted))),
            )

            GjennomforingDetaljerService.tilgangTilHandling(
                ansatt,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI,
                GjennomforingType.ENKELTPLASS,
                setOf(kostnadssted),
            ) shouldBe true
            GjennomforingDetaljerService.tilgangTilHandling(
                ansatt,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI,
                GjennomforingType.ENKELTPLASS,
                setOf(NavEnhetNummer("0502")),
            ) shouldBe false
            GjennomforingDetaljerService.tilgangTilHandling(
                ansatt,
                GjennomforingHandling.GODKJENN_ENKELTPLASS_OKONOMI,
                GjennomforingType.ARENA,
                setOf(kostnadssted),
            ) shouldBe false
        }
    }

    context("getGjennomforingDetaljerDto") {
        test("returnerer detaljer for en gruppetiltak-gjennomføring (AVTALE)") {
            val db = TestAdminDatabase()
            val gjennomforing = GjennomforingFixtures.createGjennomforingAvtale(navn = "Oppfølging 1")
            every { db.repository.gjennomforing.getGjennomforing(gjennomforing.id) } returns gjennomforing
            every {
                db.queries.gjennomforing.getGjennomforingAvtaleDetaljerOrError(gjennomforing.id)
            } returns GjennomforingFixtures.createGjennomforingAvtaleDetaljer()
            val service = createService(db)

            val dto = service.getGjennomforingDetaljerDto(
                gjennomforing.id,
                AccessType.OBO.AzureAd("X123456"),
                NavIdent("Z123456"),
            ).shouldBeRight().shouldNotBeNull().shouldBeTypeOf<GjennomforingAvtaleDetaljerDto>()

            dto.gjennomforing.type shouldBe GjennomforingAvtaleDto.Type.AVTALE
            dto.gjennomforing.id shouldBe gjennomforing.id
            dto.gjennomforing.navn shouldBe gjennomforing.navn
        }

        test("returnerer detaljer om deltaker for en enkeltplass-gjennomføring") {
            val db = TestAdminDatabase()
            val gjennomforing = GjennomforingFixtures.createGjennomforingEnkeltplass()
            val deltaker = DeltakerFixtures.createDeltaker(
                gjennomforingId = gjennomforing.id,
                startDato = LocalDate.of(2025, 1, 1),
                sluttDato = LocalDate.of(2025, 2, 1),
                status = DeltakerStatusType.DELTAR,
            )
            val personaliaPort = mockk<PersonaliaPort>()
            val enkeltplassPort = mockk<EnkeltplassPort>()
            val auditLog = mockk<GjennomforingAuditLog>(relaxed = true)
            every { db.repository.gjennomforing.getGjennomforing(gjennomforing.id) } returns gjennomforing
            every { enkeltplassPort.get(gjennomforing.id) } returns Enkeltplass(gjennomforing, null, null)
            db.repository.deltaker.save(deltaker)
            coEvery { personaliaPort.getPersonalia(deltaker.id, any()) } returns getPersonalia(
                deltaker,
                gradering = Gradering.UGRADERT,
                avvist = null,
            )
            every { db.queries.opplaering.get(any()) } returns null
            val service = createService(db, personaliaPort, enkeltplassPort, auditLog)

            val dto = service.getGjennomforingDetaljerDto(
                gjennomforing.id,
                AccessType.OBO.AzureAd("X123456"),
                NavIdent("Z123456"),
            ).shouldBeRight().shouldNotBeNull().shouldBeTypeOf<GjennomforingEnkeltplassDetaljerDto>()

            dto.gjennomforing.type shouldBe GjennomforingEnkeltplassDto.Type.ENKELTPLASS
            dto.gjennomforing.id shouldBe gjennomforing.id
            dto.deltaker.shouldNotBeNull().should {
                it.navn shouldBe "Test Testesen"
                it.norskIdent shouldBe NorskIdent("12345678901")
                it.avvistGrunn.shouldBeNull()
            }
            verify {
                auditLog.visEnkeltplassDeltaker(gjennomforing.id, NavIdent("Z123456"), NorskIdent("12345678901"))
            }
        }

        test("returnerer avvist grunn når deltaker er adressebeskyttet") {
            val db = TestAdminDatabase()
            val gjennomforing = GjennomforingFixtures.createGjennomforingEnkeltplass()
            val deltaker = DeltakerFixtures.createDeltaker(gjennomforingId = gjennomforing.id)
            val personaliaPort = mockk<PersonaliaPort>()
            val enkeltplassPort = mockk<EnkeltplassPort>()
            val auditLog = mockk<GjennomforingAuditLog>(relaxed = true)
            every { db.repository.gjennomforing.getGjennomforing(gjennomforing.id) } returns gjennomforing
            every { enkeltplassPort.get(gjennomforing.id) } returns Enkeltplass(gjennomforing, null, null)
            db.repository.deltaker.save(deltaker)
            coEvery { personaliaPort.getPersonalia(deltaker.id, any()) } returns getPersonalia(
                deltaker,
                gradering = Gradering.STRENGT_FORTROLIG_ADRESSE,
                avvist = AvvistGrunn.AVVIST_FORTROLIG_ADRESSE,
            )
            every { db.queries.opplaering.get(any()) } returns null
            val service = createService(db, personaliaPort, enkeltplassPort, auditLog)

            service.getGjennomforingDetaljerDto(
                gjennomforing.id,
                AccessType.OBO.AzureAd("X123456"),
                NavIdent("Z123456"),
            ).shouldBeLeft() shouldBe AvvistGrunn.AVVIST_FORTROLIG_ADRESSE
        }

        test("returnerer null når gjennomføring ikke finnes") {
            val db = TestAdminDatabase()
            val id = UUID.randomUUID()
            every { db.repository.gjennomforing.getGjennomforing(id) } returns null
            val service = createService(db)

            service.getGjennomforingDetaljerDto(
                id,
                AccessType.OBO.AzureAd("X123456"),
                NavIdent("Z123456"),
            ).shouldBeRight().shouldBeNull()
        }
    }

    context("getAllKompaktDto") {
        test("kan søke på navn av gruppetiltak") {
            val db = TestAdminDatabase()
            val kompakt = GjennomforingFixtures.createGjennomforingAvtaleKompakt(
                navn = "Oppfølging 1",
            )
            every {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = "Oppfølging",
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            } returns PaginatedResult(1, listOf(kompakt))
            val service = createService(db)

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = "Oppfølging"),
                accessType = AccessType.OBO.AzureAd("X123456"),
            ).shouldBeRight()

            result.items shouldHaveSize 1
            result.items.first().id shouldBe kompakt.id
            verify {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = "Oppfølging",
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            }
        }

        test("kan søke på norskIdent for enkeltplass-gjennomføring") {
            val db = TestAdminDatabase()
            val personaliaPort = mockk<PersonaliaPort>()
            val accessType = AccessType.OBO.AzureAd("X123456")
            val kompakt = GjennomforingFixtures.createGjennomforingEnkeltplassKompakt()
            coEvery { personaliaPort.navAnsattTilgangTilPerson(norskIdent, accessType) } returns null
            every {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = any(),
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            } returns PaginatedResult(1, listOf(kompakt))
            val service = createService(db, personaliaPort)

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = norskIdent.value),
                accessType = accessType,
            ).shouldBeRight()

            result.items shouldHaveSize 1
            result.items.first().id shouldBe kompakt.id
            verify {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = NorskIdentHasher.hashIfNorskIdent(norskIdent.value),
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            }
        }

        test("kan ikke søke på norskIdent hvis ikke tilgang") {
            val db = TestAdminDatabase()
            val personaliaPort = mockk<PersonaliaPort>()
            val accessType = AccessType.OBO.AzureAd("X123456")
            coEvery {
                personaliaPort.navAnsattTilgangTilPerson(norskIdent, accessType)
            } returns AvvistGrunn.AVVIST_FORTROLIG_ADRESSE
            val service = createService(db, personaliaPort)

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = norskIdent.value),
                accessType = accessType,
            ).shouldBeLeft()

            result shouldBe AvvistGrunn.AVVIST_FORTROLIG_ADRESSE
            verify(exactly = 0) {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = any(),
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            }
        }

        test("returnerer tomt resultat ved søk som ikke matcher noe") {
            val db = TestAdminDatabase()
            every {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = "finnesikke",
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            } returns PaginatedResult(0, emptyList())
            val service = createService(db)

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = "finnesikke"),
                accessType = AccessType.OBO.AzureAd("X123456"),
            ).shouldBeRight()

            result.items shouldHaveSize 0
        }

        test("returnerer alle gjennomføringer uten søkefilter") {
            val db = TestAdminDatabase()
            val items = listOf(
                GjennomforingFixtures.createGjennomforingAvtaleKompakt(),
                GjennomforingFixtures.createGjennomforingEnkeltplassKompakt(),
            )
            every {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = any(),
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            } returns PaginatedResult(2, items)
            val service = createService(db)

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(),
                accessType = AccessType.OBO.AzureAd("X123456"),
            ).shouldBeRight()

            result.items shouldHaveSize 2
        }

        test("kan generere excel for gjennomføringer") {
            val db = TestAdminDatabase()
            val kompakt = GjennomforingFixtures.createGjennomforingAvtaleKompakt(navn = "Oppfølging 1")
            every {
                db.queries.gjennomforing.getAll(
                    pagination = any(),
                    search = "Oppfølging",
                    navEnheter = any(),
                    tiltakstyper = any(),
                    statuser = any(),
                    sortering = any(),
                    sluttDatoGreaterThanOrEqualTo = any(),
                    avtaleId = any(),
                    arrangorIds = any(),
                    arrangorOrgnr = any(),
                    administratorNavIdent = any(),
                    publisert = any(),
                    koordinatorNavIdent = any(),
                    typer = any(),
                )
            } returns PaginatedResult(1, listOf(kompakt))
            val service = createService(db)

            val file = service.exportToExcel(
                filter = AdminTiltaksgjennomforingFilter(search = "Oppfølging"),
                accessType = AccessType.OBO.AzureAd("X123456"),
            ).shouldBeRight()

            WorkbookFactory.create(file.inputStream()).use { workbook ->
                val sheet = workbook.getSheetAt(0)

                sheet.getRow(0).getCell(0).stringCellValue shouldBe "Tiltaksnavn"
                sheet.getRow(0).getCell(1).stringCellValue shouldBe "Tiltakstype"

                sheet.lastRowNum shouldBe 1
                sheet.getRow(1).getCell(0).stringCellValue shouldBe "Oppfølging 1"
            }
        }
    }
})

private fun getPersonalia(deltaker: Deltaker, gradering: Gradering, avvist: AvvistGrunn?): Personalia = Personalia(
    deltakerId = deltaker.id,
    norskIdent = NorskIdent("12345678901"),
    navn = "Test Testesen",
    oppfolgingEnhet = null,
    geografiskEnhet = null,
    region = null,
    gradering = gradering,
    avvistGrunn = avvist,
)
