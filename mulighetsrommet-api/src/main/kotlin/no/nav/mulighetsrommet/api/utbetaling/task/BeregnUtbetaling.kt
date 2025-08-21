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
import no.nav.mulighetsrommet.api.services.ExcelWorkbookBuilder
import no.nav.mulighetsrommet.api.services.buildExcelWorkbook
import no.nav.mulighetsrommet.api.utbetaling.GenererUtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
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
): XSSFWorkbook = buildExcelWorkbook {
    createUtbetalingerSheet(
        "Utbetaling",
        getDifference(existingUtbetalinger, newUtbetalinger),
    )
    createUtbetalingerSheet(
        "Ny beregning",
        getDifference(newUtbetalinger, existingUtbetalinger),
    )
}

private fun getDifference(
    source: List<Utbetaling>,
    other: List<Utbetaling>,
): List<Utbetaling> {
    val otherById = other.associateBy { it.gjennomforing.id }
    return source.filter { sourceEntry ->
        val otherEntry = otherById[sourceEntry.gjennomforing.id]
        otherEntry == null || sourceEntry.beregning.output.belop != otherEntry.beregning.output.belop
    }
}

private fun ExcelWorkbookBuilder.createUtbetalingerSheet(
    sheetName: String,
    utbetalinger: List<Utbetaling>,
) = sheet(sheetName) {
    header(
        "Tiltakskode",
        "Gjennomføring - Id",
        "Gjennomføring - Navn",
        "Utbetaling - Beregning",
        "Utbetaling - Periode",
        "Utbetaling - Beløp",
        "Deltakelse - Id",
        "Deltakelse - Faktor",
    )

    val utbetalingComparator = compareBy<Utbetaling>({ it.tiltakstype.tiltakskode }, { it.gjennomforing.navn })

    utbetalinger.sortedWith(utbetalingComparator).forEach { utbetaling ->
        utbetaling.beregning.output.deltakelser().sortedBy { it.deltakelseId }.forEach { deltakelse ->
            row(
                utbetaling.tiltakstype.tiltakskode,
                utbetaling.gjennomforing.id,
                utbetaling.gjennomforing.navn,
                utbetaling.beregning::class.simpleName!!,
                utbetaling.periode.formatPeriode(),
                utbetaling.beregning.output.belop,
                deltakelse.deltakelseId,
                deltakelse.faktor,
            )
        }
    }
}
