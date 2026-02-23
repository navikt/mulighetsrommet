package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.model.ValutaBelop
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

fun buildExcelWorkbook(init: ExcelWorkbookBuilder.() -> Unit): XSSFWorkbook {
    val builder = ExcelWorkbookBuilder()
    builder.init()
    return builder.build()
}

class ExcelWorkbookBuilder {
    private val workbook = XSSFWorkbook()

    fun sheet(
        name: String,
        autoSizeColumns: Boolean = true,
        init: ExcelSheetBuilder.() -> Unit,
    ) {
        val sheet = workbook.createSheet(name)
        ExcelSheetBuilder(sheet, workbook).also {
            it.init()

            if (autoSizeColumns) {
                it.autoSizeAllColumns()
            }
        }
    }

    fun build(): XSSFWorkbook = workbook
}

class ExcelSheetBuilder(
    private val sheet: XSSFSheet,
    private val workbook: XSSFWorkbook,
) {
    private var currentRow = 0

    private val styleCache = mutableMapOf<IndexedColors, XSSFCellStyle>()

    private fun getStyle(color: IndexedColors): XSSFCellStyle {
        return styleCache.getOrPut(color) {
            val style = workbook.createCellStyle()
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
            style.fillForegroundColor = color.index
            style
        }
    }

    fun header(vararg titles: String) {
        val row = sheet.createRow(currentRow++)
        val style = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.THIN
        }
        titles.forEachIndexed { idx, title ->
            val cell = row.createCell(idx, CellType.STRING)
            cell.setCellValue(title)
            cell.cellStyle = style
        }
    }

    fun row(color: IndexedColors? = null, block: () -> List<Any?>) {
        val row = sheet.createRow(currentRow++)
        block().forEachIndexed { idx, value ->
            val cell = row.createCell(idx)
            when (value) {
                null -> cell.setBlank()

                is Number -> {
                    cell.setCellValue(value.toDouble())
                    cell.cellType = CellType.NUMERIC
                }

                is Boolean -> {
                    cell.setCellValue(value)
                    cell.cellType = CellType.BOOLEAN
                }

                is ValutaBelop -> {
                    cell.setCellValue(value.belop.toDouble())
                    cell.cellType = CellType.NUMERIC
                }

                else -> {
                    cell.setCellValue(value.toString())
                    cell.cellType = CellType.STRING
                }
            }

            color?.also {
                cell.cellStyle = getStyle(it)
            }
        }
    }

    fun autoSizeAllColumns() {
        val maxColumns = (0..sheet.lastRowNum)
            .mapNotNull { sheet.getRow(it)?.lastCellNum?.toInt() }
            .maxOrNull() ?: 0
        repeat(maxColumns) { sheet.autoSizeColumn(it) }
    }
}
