package no.nav.mulighetsrommet.excel

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import kotlin.io.path.outputStream

class ExcelServiceTest : FunSpec({
    test("Opprett Excel-fil oppretter fil") {
        val workbook = XSSFWorkbook()
        val workSheet = workbook.createSheet()
        val cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = IndexedColors.RED.getIndex()
        cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        val firstCell = workSheet
            .createRow(0)
            .createCell(0)
        firstCell.setCellValue("SAVED VALUE")
        firstCell.cellStyle = cellStyle

        val tempFile = kotlin.io.path.createTempFile("test_output_", ".xlsx")
        workbook.write(tempFile.outputStream())
        workbook.close()

        val inputWorkbook = WorkbookFactory.create(tempFile.toFile().inputStream())
        val firstSheet = inputWorkbook.getSheetAt(0)
        firstSheet.getRow(0).getCell(0).stringCellValue shouldBe "SAVED VALUE"
    }
})
