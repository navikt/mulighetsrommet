package no.nav.mulighetsrommet.excel

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.services.ExcelService
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory

class ExcelServiceTest : FunSpec({
    test("Opprett Excel-fil oppretter fil") {
        val tempFile = ExcelService.createExcelFile {
            val workSheet = this.createSheet()
            val cellStyle = this.createCellStyle()
            cellStyle.fillForegroundColor = IndexedColors.RED.getIndex()
            cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            val firstCell = workSheet
                .createRow(0)
                .createCell(0)
            firstCell.setCellValue("SAVED VALUE")
            firstCell.cellStyle = cellStyle
        }

        val inputWorkbook = WorkbookFactory.create(tempFile.inputStream())
        val firstSheet = inputWorkbook.getSheetAt(0)
        firstSheet.getRow(0).getCell(0).stringCellValue shouldBe "SAVED VALUE"
    }
})
