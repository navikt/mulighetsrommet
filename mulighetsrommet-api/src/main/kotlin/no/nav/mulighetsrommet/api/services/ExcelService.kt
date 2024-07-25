package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.io.path.outputStream

class ExcelService {
    fun createExcelFileForAvtale(
        result: List<AvtaleAdminDto>,
    ): File {
        val workbook = XSSFWorkbook()
        val workSheet = workbook.createSheet()
        val headers = workSheet.createRow(0)
        opprettHeadersForAvtale(headers)
        result.forEachIndexed { index, avtaleAdminDto ->
            val row = workSheet.createRow(index + 1)
            opprettCelle(row, 0, avtaleAdminDto.navn)
            opprettCelle(row, 1, avtaleAdminDto.tiltakstype.navn)
            opprettCelle(row, 2, avtaleAdminDto.avtalenummer ?: "")
            opprettCelle(row, 3, avtaleAdminDto.arrangor.navn)
            opprettCelle(row, 4, avtaleAdminDto.arrangor.organisasjonsnummer)
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

    fun createExcelFileForTiltaksgjennomforing(
        result: List<TiltaksgjennomforingAdminDto>,
    ): File {
        val workbook = XSSFWorkbook()
        val workSheet = workbook.createSheet()
        val headers = workSheet.createRow(0)
        opprettHeadersForTiltaksgjennomforing(headers)
        result.forEachIndexed { index, tiltaksgjennomforingAdminDto ->
            val row = workSheet.createRow(index + 1)
            opprettCelle(row, 0, tiltaksgjennomforingAdminDto.navn)
            opprettCelle(row, 1, tiltaksgjennomforingAdminDto.tiltakstype.navn)
            opprettCelle(row, 2, tiltaksgjennomforingAdminDto.tiltaksnummer ?: "")
            opprettCelle(row, 3, tiltaksgjennomforingAdminDto.arrangor.navn)
            opprettCelle(row, 4, tiltaksgjennomforingAdminDto.arrangor.organisasjonsnummer)
            opprettCelle(
                row,
                5,
                tiltaksgjennomforingAdminDto.startDato.formaterDato(),
            )
            opprettCelle(
                row,
                6,
                tiltaksgjennomforingAdminDto.sluttDato?.formaterDato() ?: "",
            )
        }

        val tempFile = kotlin.io.path.createTempFile("avtaler", ".xlsx")
        workbook.write(tempFile.outputStream())
        workbook.close()

        return tempFile.toFile()
    }

    private fun opprettHeadersForAvtale(headers: XSSFRow) {
        opprettCelle(headers, 0, "Avtalenavn")
        opprettCelle(headers, 1, "Tiltakstype")
        opprettCelle(headers, 2, "Avtalenummer")
        opprettCelle(headers, 3, "Tiltaksarrangør")
        opprettCelle(headers, 4, "Tiltaksarrangør orgnr")
        opprettCelle(headers, 5, "Startdato")
        opprettCelle(headers, 6, "Sluttdato")
    }

    private fun opprettHeadersForTiltaksgjennomforing(headers: XSSFRow) {
        opprettCelle(headers, 0, "Tiltaksnavn")
        opprettCelle(headers, 1, "Tiltakstype")
        opprettCelle(headers, 2, "Tiltaksnummer")
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
