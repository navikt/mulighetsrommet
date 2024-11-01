package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.utils.DatoUtils.formaterDatoTilEuropeiskDatoformat
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
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

    fun createExcelFileForAvtale(result: List<AvtaleDto>): File =
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

            result.forEachIndexed { index, avtale ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(avtale.navn)
                row.createCell(1).setCellValue(avtale.tiltakstype.navn)
                row.createCell(2).setCellValue(avtale.avtalenummer ?: "")
                row.createCell(3).setCellValue(avtale.arrangor.navn)
                row.createCell(4).setCellValue(avtale.arrangor.organisasjonsnummer.value)
                row.createCell(5).setCellValue(avtale.startDato.formaterDatoTilEuropeiskDatoformat())
                row.createCell(6).setCellValue(avtale.sluttDato?.formaterDatoTilEuropeiskDatoformat() ?: "")
            }
        }

    fun createExcelFileForTiltaksgjennomforing(result: List<TiltaksgjennomforingDto>): File =
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

            result.forEachIndexed { index, tiltak ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(tiltak.navn)
                row.createCell(1).setCellValue(tiltak.tiltakstype.navn)
                row.createCell(2).setCellValue(tiltak.tiltaksnummer ?: "")
                row.createCell(3).setCellValue(tiltak.arrangor.navn)
                row.createCell(4).setCellValue(tiltak.arrangor.organisasjonsnummer.value)
                row.createCell(5)
                    .setCellValue(tiltak.startDato.formaterDatoTilEuropeiskDatoformat())
                row.createCell(6)
                    .setCellValue(tiltak.sluttDato?.formaterDatoTilEuropeiskDatoformat() ?: "")
            }
        }
}
