package no.nav.mulighetsrommet.api.utbetaling.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.*
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

class BeregnUtbetaling(
    private val config: Config,
    private val genererUtbetalingService: GenererUtbetalingService,
    private val db: ApiDatabase,
) {

    @Serializable
    data class Input(
        val periode: Periode,
    )

    data class Config(
        /**
         * Rapport blir lastet opp til respektiv GCP bucket, evt. skrevet som en tmp-fil om ikke [bucketName] er satt.
         */
        val bucketName: String? = null,
    )

    private val storage: Storage = StorageOptions.getDefaultInstance().service

    val task: OneTimeTask<Input> = Tasks
        .oneTime(javaClass.simpleName, Input::class.java)
        .executeSuspend { instance, _ ->
            val input = instance.data
            val existingUtbetalinger = db.session { queries.utbetaling.getByPeriode(input.periode) }
            val newUtbetalinger = genererUtbetalingService.beregnUtbetalingerForPeriode(input.periode)
            val report = createReport(existingUtbetalinger, newUtbetalinger)
            upload(report)
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(data: Input, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), data)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    private suspend fun upload(report: XSSFWorkbook) {
        if (config.bucketName != null) {
            uploadToBucket(config.bucketName, report)
        } else {
            writeToTempFile(report)
        }
    }

    private suspend fun uploadToBucket(bucketName: String, report: XSSFWorkbook) = withContext(Dispatchers.IO) {
        val blobName = "utbetalinger/utbetaling-${System.currentTimeMillis()}.xlsx"

        val bucket = storage.get(bucketName) ?: error("Bucket $bucketName does not exist.")

        report
            .use { workbook ->
                ByteArrayOutputStream().use { data ->
                    workbook.write(data)
                    ByteArrayInputStream(data.toByteArray())
                }
            }
            .use { bucket.create(blobName, it) }
    }

    private fun writeToTempFile(report: XSSFWorkbook) {
        report.use {
            val file = createTempFile("utbetalinger-", ".xlsx")
            report.write(file.outputStream())
        }
    }
}

private fun createReport(
    existingUtbetalinger: List<Utbetaling>,
    newUtbetalinger: List<Utbetaling>,
): XSSFWorkbook {
    val workbook = XSSFWorkbook()

    createUtbetalingerSheet(
        workbook,
        "Utbetaling",
        getDifference(existingUtbetalinger, newUtbetalinger),
    )
    createUtbetalingerSheet(
        workbook,
        "Ny beregning",
        getDifference(newUtbetalinger, existingUtbetalinger),
    )

    return workbook
}

private fun getDifference(
    source: List<Utbetaling>,
    other: List<Utbetaling>,
): List<Utbetaling> {
    val otherById = other.associateBy { it.gjennomforing.id }
    return source.filter { sourceEntry ->
        val otherEntry = otherById[sourceEntry.gjennomforing.id]
        otherEntry == null || sourceEntry.beregning != otherEntry.beregning
    }
}

private fun createUtbetalingerSheet(
    workbook: XSSFWorkbook,
    sheetName: String,
    utbetalinger: List<Utbetaling>,
) {
    val workSheet = workbook.createSheet(sheetName)
    createHeader(workSheet)

    val utbetalingComparator = compareBy<Utbetaling>({ it.tiltakstype.tiltakskode }, { it.gjennomforing.navn })

    var rowNumber = 1
    utbetalinger
        .sortedWith(utbetalingComparator)
        .forEach { utbetaling ->
            utbetaling.beregning.output.deltakelser()
                .sortedBy { it.deltakelseId }
                .forEach { deltakelse ->
                    createRow(
                        workSheet,
                        rowNumber++,
                        tiltakskode = utbetaling.tiltakstype.tiltakskode,
                        gjennomforingId = utbetaling.gjennomforing.id,
                        gjennomforingNavn = utbetaling.gjennomforing.navn,
                        beregning = utbetaling.beregning::class.simpleName!!,
                        periode = utbetaling.periode,
                        belopBeregnet = utbetaling.beregning.output.belop,
                        deltakelseId = deltakelse.deltakelseId,
                        deltakelseFaktor = deltakelse.faktor,
                    )
                }
        }

    workSheet.autoSizeColumn(0)
    workSheet.autoSizeColumn(1)
    workSheet.autoSizeColumn(2)
    workSheet.autoSizeColumn(3)
    workSheet.autoSizeColumn(4)
    workSheet.autoSizeColumn(5)
    workSheet.autoSizeColumn(6)
    workSheet.autoSizeColumn(7)
}

private fun createHeader(workSheet: XSSFSheet) {
    val header = workSheet.createRow(0)
    header.createCell(0, CellType.STRING).setCellValue("Tiltakskode")
    header.createCell(1, CellType.STRING).setCellValue("Gjennomføring - id")
    header.createCell(2, CellType.STRING).setCellValue("Gjennomføring - navn")
    header.createCell(3, CellType.STRING).setCellValue("Utbetaling - beregning")
    header.createCell(4, CellType.STRING).setCellValue("Utbetaling - periode")
    header.createCell(5, CellType.STRING).setCellValue("Utbetaling - beløp")
    header.createCell(6, CellType.STRING).setCellValue("Deltakelse - id")
    header.createCell(7, CellType.STRING).setCellValue("Deltakelse - faktor")
}

private fun createRow(
    workSheet: XSSFSheet,
    rowNumber: Int,
    tiltakskode: Tiltakskode,
    gjennomforingId: UUID,
    gjennomforingNavn: String,
    beregning: String,
    periode: Periode,
    belopBeregnet: Int,
    deltakelseId: UUID,
    deltakelseFaktor: Double,
) {
    val row = workSheet.createRow(rowNumber)
    row.createCell(0, CellType.STRING).setCellValue(tiltakskode.name)
    row.createCell(1, CellType.STRING).setCellValue(gjennomforingId.toString())
    row.createCell(2, CellType.STRING).setCellValue(gjennomforingNavn)
    row.createCell(3, CellType.STRING).setCellValue(beregning)
    row.createCell(4, CellType.STRING).setCellValue(periode.formatPeriode())
    row.createCell(5, CellType.NUMERIC).setCellValue(belopBeregnet.toDouble())
    row.createCell(6, CellType.STRING).setCellValue(deltakelseId.toString())
    row.createCell(7, CellType.NUMERIC).setCellValue(deltakelseFaktor)
}
