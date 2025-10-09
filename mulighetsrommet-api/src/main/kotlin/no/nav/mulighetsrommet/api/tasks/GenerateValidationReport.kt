package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.mapper.prisbetingelser
import no.nav.mulighetsrommet.api.avtale.mapper.satser
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.gjennomforing.mapper.GjennomforingDboMapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingValidator
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

class GenerateValidationReport(
    private val config: Config,
    private val db: ApiDatabase,
    private val avtaleValidator: AvtaleValidator,
    private val gjennomforingService: GjennomforingService,
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

    private suspend fun validateAvtaler(): Map<Avtale, List<FieldError>> = db.session {
        buildMap {
            paginateFanOut({ pagination -> queries.avtale.getAll(pagination).items }) { dto ->
                avtaleValidator.validate(dto.toAvtaleRequest(), dto).onLeft { validationErrors ->
                    put(dto, validationErrors)
                }
            }
        }
    }

    private fun createAvtalerSheet(
        workbook: XSSFWorkbook,
        result: Map<Avtale, List<FieldError>>,
    ) {
        val workSheet = workbook.createSheet("Avtaler")
        createHeader(workSheet)

        var rowNumber = 1
        result.forEach { (dto, errors) ->
            errors.forEach { error ->
                createRow(workSheet, rowNumber++, dto.id, dto.navn, dto.opphav.name, dto.status.type.name, error)
            }
        }
    }

    private suspend fun validateGjennomforinger(): Map<Gjennomforing, List<FieldError>> = db.session {
        buildMap {
            paginateFanOut({ pagination ->
                queries.gjennomforing.getAll(
                    pagination,
                    sluttDatoGreaterThanOrEqualTo = ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate,
                ).items
            }) {
                val request = GjennomforingDboMapper.toGjennomforingRequest(it)
                val ctx = gjennomforingService.getValidatorCtx(request, it, LocalDate.now())
                GjennomforingValidator.validate(request, ctx).onLeft { validationErrors ->
                    put(it, validationErrors)
                }
            }
        }
    }

    private fun createGjennomforingerSheet(
        workbook: XSSFWorkbook,
        result: Map<Gjennomforing, List<FieldError>>,
    ) {
        val workSheet = workbook.createSheet("Gjennomføringer")
        createHeader(workSheet)

        var rowNumber = 1
        result.forEach { (dto, errors) ->
            errors.forEach { error ->
                createRow(workSheet, rowNumber++, dto.id, dto.navn, dto.opphav.name, dto.status.type.name, error)
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
        error: FieldError,
    ) {
        val row = workSheet.createRow(rowNumber)
        row.createCell(0, CellType.STRING).setCellValue(uuid.toString())
        row.createCell(1, CellType.STRING).setCellValue(navn)
        row.createCell(2, CellType.STRING).setCellValue(opphav)
        row.createCell(3, CellType.STRING).setCellValue(status)
        row.createCell(4, CellType.STRING).setCellValue(error.pointer)
        row.createCell(5, CellType.STRING).setCellValue(error.detail)
    }
}

fun Avtale.toAvtaleRequest() = AvtaleRequest(
    id = this.id,
    navn = this.navn,
    tiltakskode = this.tiltakstype.tiltakskode,
    arrangor = this.arrangor?.let {
        AvtaleRequest.Arrangor(
            hovedenhet = it.organisasjonsnummer,
            underenheter = it.underenheter.map { it.organisasjonsnummer },
            kontaktpersoner = it.kontaktpersoner.map { it.id },
        )
    },
    sakarkivNummer = this.sakarkivNummer,
    startDato = this.startDato,
    sluttDato = this.sluttDato,
    administratorer = this.administratorer.map { it.navIdent },
    avtaletype = this.avtaletype,
    veilederinformasjon = VeilederinfoRequest(
        navEnheter = this.kontorstruktur.flatMap { listOf(it.region.enhetsnummer) + it.kontorer.map { it.enhetsnummer } },
        beskrivelse = this.beskrivelse,
        faneinnhold = this.faneinnhold,
    ),
    personvern = PersonvernRequest(
        personopplysninger = this.personopplysninger,
        personvernBekreftet = this.personvernBekreftet,
    ),
    opsjonsmodell = this.opsjonsmodell,
    amoKategorisering = this.amoKategorisering,
    utdanningslop = this.utdanningslop?.toDbo(),
    prismodell = PrismodellRequest(
        type = this.prismodell.type,
        satser = this.prismodell.satser().map {
            AvtaltSatsRequest(
                pris = it.sats,
                gjelderFra = it.gjelderFra,
                valuta = "NOK",
            )
        },
        prisbetingelser = this.prismodell.prisbetingelser(),
    ),
)
