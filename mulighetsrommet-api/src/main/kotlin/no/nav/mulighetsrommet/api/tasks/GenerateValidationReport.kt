package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.*
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

class GenerateValidationReport(
    private val config: Config,
    private val db: Database,
    private val avtaleValidator: AvtaleValidator,
    private val gjennomforingValidator: TiltaksgjennomforingValidator,
) {

    data class Config(
        /**
         * Rapport blir lastet opp til respektiv GCP bucket, evt. skrevet som en tmp-fil om ikke [bucketName] er satt.
         */
        val bucketName: String? = null,
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName)
        .executeSuspend { _, _ ->
            val report = createReport()
            upload(report)
        }

    private val client = SchedulerClient.Builder.create(db.getDatasource(), task).build()

    private val storage: Storage = StorageOptions.getDefaultInstance().service

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        client.scheduleIfNotExists(task.instance(id.toString()), startTime)
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
        val blobName = "validation-reports/report-${System.currentTimeMillis()}.xlsx"

        logger.info("Uploading file $blobName to bucket $bucketName")

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
            val file = createTempFile("report", ".xlsx")
            logger.info("Skriver rapport til fil ${file.fileName}")
            report.write(file.outputStream())
        }
    }

    private suspend fun createReport(): XSSFWorkbook {
        val workbook = XSSFWorkbook()

        val avtaler = validateAvtaler()
        createAvtalerSheet(workbook, avtaler)

        val gjennnomforingerResult = validateGjennomforinger()
        createGjennomforingerSheet(workbook, gjennnomforingerResult)

        return workbook
    }

    private suspend fun validateAvtaler(): Map<AvtaleDto, List<ValidationError>> = db.session {
        buildMap {
            paginateFanOut({ pagination -> Queries.avtale.getAll(pagination).items }) {
                avtaleValidator.validate(it.toDbo(), it).onLeft { validationErrors ->
                    put(it, validationErrors)
                }
            }
        }
    }

    private fun createAvtalerSheet(
        workbook: XSSFWorkbook,
        result: Map<AvtaleDto, List<ValidationError>>,
    ) {
        val workSheet = workbook.createSheet("Avtaler")
        createHeader(workSheet)

        var rowNumber = 1
        result.forEach { (dto, errors) ->
            errors.forEach { error ->
                createRow(workSheet, rowNumber++, dto.id, dto.navn, dto.opphav.name, dto.status.enum.name, error)
            }
        }
    }

    private suspend fun validateGjennomforinger(): Map<TiltaksgjennomforingDto, List<ValidationError>> = db.session {
        buildMap {
            paginateFanOut({ pagination ->
                Queries.gjennomforing.getAll(
                    pagination,
                    sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
                ).items
            }) {
                gjennomforingValidator.validate(it.toTiltaksgjennomforingDbo(), it).onLeft { validationErrors ->
                    put(it, validationErrors)
                }
            }
        }
    }

    private fun createGjennomforingerSheet(
        workbook: XSSFWorkbook,
        result: Map<TiltaksgjennomforingDto, List<ValidationError>>,
    ) {
        val workSheet = workbook.createSheet("Gjennomføringer")
        createHeader(workSheet)

        var rowNumber = 1
        result.forEach { (dto, errors) ->
            errors.forEach { error ->
                createRow(workSheet, rowNumber++, dto.id, dto.navn, dto.opphav.name, dto.status.status.name, error)
            }
        }
    }

    private fun createHeader(workSheet: XSSFSheet) {
        val header = workSheet.createRow(0)
        header.createCell(0, CellType.STRING).setCellValue("ID")
        header.createCell(1, CellType.STRING).setCellValue("Navn")
        header.createCell(2, CellType.STRING).setCellValue("Opphav")
        header.createCell(3, CellType.STRING).setCellValue("Status")
        header.createCell(4, CellType.STRING).setCellValue("Path")
        header.createCell(5, CellType.STRING).setCellValue("Message")
    }

    private fun createRow(
        workSheet: XSSFSheet,
        rowNumber: Int,
        uuid: UUID,
        navn: String,
        opphav: String,
        status: String,
        error: ValidationError,
    ) {
        val row = workSheet.createRow(rowNumber)
        row.createCell(0, CellType.STRING).setCellValue(uuid.toString())
        row.createCell(1, CellType.STRING).setCellValue(navn)
        row.createCell(2, CellType.STRING).setCellValue(opphav)
        row.createCell(3, CellType.STRING).setCellValue(status)
        row.createCell(4, CellType.STRING).setCellValue(error.name)
        row.createCell(5, CellType.STRING).setCellValue(error.message)
    }
}
