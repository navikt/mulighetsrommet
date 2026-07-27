package no.nav.mulighetsrommet.admin.tiltak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.mulighetsrommet.admin.testing.AvtaleDtoFixtures.createAvtaleDto
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Tiltakskode
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.util.UUID

class AvtaleDtoQueryTest : FunSpec({

    fun createAvtaleDtoQuery(db: TestAdminDatabase): AvtaleDtoQuery {
        return AvtaleDtoQuery(db, TiltakstypeService(db = db))
    }

    context("hent avtale") {
        test("returnerer avtale når den finnes") {
            val db = TestAdminDatabase()
            val avtale = createAvtaleDto()
            every { db.queries.avtale.getAvtaleDto(avtale.id) } returns avtale

            val query = createAvtaleDtoQuery(db)

            query.execute(GetAvtaleDto(avtale.id)) shouldBe avtale
        }

        test("kaster NoSuchElementException når avtalen ikke finnes") {
            val db = TestAdminDatabase()
            val id = UUID.randomUUID()
            every { db.queries.avtale.getAvtaleDto(id) } returns null

            val query = createAvtaleDtoQuery(db)

            shouldThrow<NoSuchElementException> {
                query.execute(GetAvtaleDto(id))
            }
        }
    }

    context("hent alle avtaler") {
        test("slår opp tiltakstype-ider basert på tiltakskoder og videresender øvrige filtre") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)

            val avtale = createAvtaleDto()
            every {
                db.queries.avtale.getAllAvtaleDto(
                    pagination = Pagination.of(2, 10),
                    tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.id),
                    search = "fretex",
                )
            } returns PaginatedResult(1, listOf(avtale))

            val query = createAvtaleDtoQuery(db)

            val result = query.execute(
                GetAllAvtaleDto(
                    pagination = Pagination.of(2, 10),
                    filter = AvtaleFilter(
                        tiltakskoder = listOf(Tiltakskode.OPPFOLGING),
                        search = "fretex",
                    ),
                ),
            )

            result shouldBe PaginatedResult(1, listOf(avtale))
        }

        test("gir tom liste med tiltakstyper når ingen tiltakskoder er oppgitt i filteret") {
            val db = TestAdminDatabase()
            every { db.queries.avtale.getAllAvtaleDto() } returns PaginatedResult(0, emptyList())

            val query = createAvtaleDtoQuery(db)

            val result = query.execute(GetAllAvtaleDto())

            result shouldBe PaginatedResult(0, emptyList())
        }
    }

    context("eksporter til excel") {
        test("kan generere excel for avtaler") {
            val avtale = createAvtaleDto(navn = "Avtale hos Fretex")

            val db = TestAdminDatabase()
            every { db.queries.avtale.getAllAvtaleDto() } returns PaginatedResult(
                totalCount = 1,
                items = listOf(avtale),
            )

            val query = createAvtaleDtoQuery(db)

            val file = query.execute(GetExcelExport())

            WorkbookFactory.create(file.inputStream()).use { workbook ->
                val sheet = workbook.getSheetAt(0)

                sheet.getRow(0).getCell(0).stringCellValue shouldBe "Avtalenavn"
                sheet.getRow(0).getCell(1).stringCellValue shouldBe "Tiltakstype"

                sheet.lastRowNum shouldBe 1
                sheet.getRow(1).getCell(0).stringCellValue shouldBe "Avtale hos Fretex"
            }
        }
    }
})
