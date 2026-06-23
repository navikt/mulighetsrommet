package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ExcelWorkbookBuilderTest : FunSpec({
    test("sheet har ikke fryst rad eller autofilter") {
        val workbook = buildExcelWorkbook {
            sheet("Test") {
                header("Kolonne A", "Kolonne B")
                row { listOf("rad1a", "rad1b") }
                row { listOf("rad2a", "rad2b") }
            }
        }

        val sheet = workbook.getSheetAt(0)
        sheet.paneInformation shouldBe null
        sheet.sheetConditionalFormatting.numConditionalFormattings shouldBe 0
        // Ingen autofilter betyr at ctWorksheet ikke har autoFilter-element
        sheet.ctWorksheet.isSetAutoFilter shouldBe false
    }

    test("table fryser header-raden og aktiverer autofilter") {
        val workbook = buildExcelWorkbook {
            table("Test") {
                header("Kolonne A", "Kolonne B", "Kolonne C")
                row { listOf("rad1a", "rad1b", "rad1c") }
                row { listOf("rad2a", "rad2b", "rad2c") }
            }
        }

        val sheet = workbook.getSheetAt(0)

        val pane = sheet.paneInformation.shouldNotBeNull()
        pane.isFreezePane shouldBe true
        pane.horizontalSplitTopRow shouldBe 1

        sheet.ctWorksheet.isSetAutoFilter shouldBe true
        val autoFilter = sheet.ctWorksheet.autoFilter
        autoFilter.ref shouldNotBe null
        // Skal dekke header + 2 datarader (rader 0..2) og 3 kolonner (A..C)
        autoFilter.ref shouldBe "A1:C3"
    }

    test("table og én datarad har korrekt autofilter-område") {
        val workbook = buildExcelWorkbook {
            table("Test") {
                header("Navn", "Verdi")
                row { listOf("foo", 42) }
            }
        }

        val sheet = workbook.getSheetAt(0)
        sheet.ctWorksheet.autoFilter.ref shouldBe "A1:B2"
    }
})
