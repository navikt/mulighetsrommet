package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.io.path.outputStream

class ExcelService {
    fun createExcelFile(result: List<AvtaleAdminDto>): File {
        val workbook = XSSFWorkbook()
        val workSheet = workbook.createSheet()
        val headers = workSheet.createRow(0)
        opprettHeaders(headers)
        result.forEachIndexed { index, avtaleAdminDto ->
            val row = workSheet.createRow(index + 1)
            opprettCelle(row, 0, avtaleAdminDto.navn)
            opprettCelle(row, 1, avtaleAdminDto.tiltakstype.navn)
            opprettCelle(row, 2, avtaleAdminDto.avtalenummer ?: "")
            opprettCelle(row, 3, avtaleAdminDto.leverandor.navn)
            opprettCelle(row, 4, avtaleAdminDto.leverandor.organisasjonsnummer)
            opprettCelle(
                row,
                5,
                avtaleAdminDto.startDato.formaterDato(),
            )
            opprettCelle(
                row,
                6,
                avtaleAdminDto.sluttDato?.formaterDato() ?: "",
            )
        }

        val tempFile = kotlin.io.path.createTempFile("avtaler", ".xlsx")
        workbook.write(tempFile.outputStream())
        workbook.close()

        return tempFile.toFile()
    }

    private fun opprettHeaders(headers: XSSFRow) {
        opprettCelle(headers, 0, "Avtalenavn")
        opprettCelle(headers, 1, "Tiltakstype")
        opprettCelle(headers, 2, "Avtalenummer")
        opprettCelle(headers, 3, "Tiltaksarrangør")
        opprettCelle(headers, 4, "Tiltaksarrangør orgnr")
        opprettCelle(headers, 5, "Startdato")
        opprettCelle(headers, 6, "Sluttdato")
    }

    private fun opprettCelle(row: XSSFRow, cellIndex: Int, verdi: String) {
        row.createCell(cellIndex).setCellValue(verdi)
    }

    private fun LocalDate.formaterDato(): String {
        return this.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    }
}
