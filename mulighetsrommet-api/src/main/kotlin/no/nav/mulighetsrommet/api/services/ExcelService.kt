package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.io.path.outputStream

object ExcelService {
    fun createExcelFile(block: XSSFWorkbook.() -> Unit): File {
        val workbook = XSSFWorkbook()
        block(workbook)

        val tempFile = kotlin.io.path.createTempFile()
        workbook.write(tempFile.outputStream())
        workbook.close()

        return tempFile.toFile()
    }

    fun createExcelFileForAvtale(result: List<AvtaleAdminDto>): File =
        createExcelFile {
            val sheet = this.createSheet()

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Avtalenavn")
            headerRow.createCell(1).setCellValue("Tiltakstype")
            headerRow.createCell(2).setCellValue("Avtalenummer")
            headerRow.createCell(3).setCellValue("Tiltaksarrangør")
            headerRow.createCell(4).setCellValue("Tiltaksarrangør orgnr")
            headerRow.createCell(5).setCellValue("Startdato")
            headerRow.createCell(6).setCellValue("Sluttdato")

            result.forEachIndexed { index, avtaleAdminDto ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(avtaleAdminDto.navn)
                row.createCell(1).setCellValue(avtaleAdminDto.tiltakstype.navn)
                row.createCell(2).setCellValue(avtaleAdminDto.avtalenummer ?: "")
                row.createCell(3).setCellValue(avtaleAdminDto.arrangor.navn)
                row.createCell(4).setCellValue(avtaleAdminDto.arrangor.organisasjonsnummer)
                row.createCell(5).setCellValue(avtaleAdminDto.startDato.formaterDatoShort())
                row.createCell(6).setCellValue(avtaleAdminDto.sluttDato?.formaterDatoShort() ?: "")
            }
        }

    fun createExcelFileForTiltaksgjennomforing(result: List<TiltaksgjennomforingAdminDto>): File =
        createExcelFile {
            val sheet = this.createSheet()

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Tiltaksnavn")
            headerRow.createCell(1).setCellValue("Tiltakstype")
            headerRow.createCell(2).setCellValue("Tiltaksnummer")
            headerRow.createCell(3).setCellValue("Tiltaksarrangør")
            headerRow.createCell(4).setCellValue("Tiltaksarrangør orgnr")
            headerRow.createCell(5).setCellValue("Startdato")
            headerRow.createCell(6).setCellValue("Sluttdato")

            result.forEachIndexed { index, tiltaksgjennomforingAdminDto ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(tiltaksgjennomforingAdminDto.navn)
                row.createCell(1).setCellValue(tiltaksgjennomforingAdminDto.tiltakstype.navn)
                row.createCell(2).setCellValue(tiltaksgjennomforingAdminDto.tiltaksnummer ?: "")
                row.createCell(3).setCellValue(tiltaksgjennomforingAdminDto.arrangor.navn)
                row.createCell(4).setCellValue(tiltaksgjennomforingAdminDto.arrangor.organisasjonsnummer)
                row.createCell(5).setCellValue(tiltaksgjennomforingAdminDto.startDato.formaterDatoShort())
                row.createCell(6).setCellValue(tiltaksgjennomforingAdminDto.sluttDato?.formaterDatoShort() ?: "")
            }
        }

    private fun LocalDate.formaterDatoShort(): String {
        return this.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    }
}
