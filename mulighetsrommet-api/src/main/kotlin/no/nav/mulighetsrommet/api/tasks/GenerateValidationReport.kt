package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingKontaktpersonDbo
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.tiltaksgjennomforinger.TiltaksgjennomforingValidator
import no.nav.mulighetsrommet.api.utils.DatabaseUtils.paginateSuspend
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import no.nav.mulighetsrommet.env.NaisEnv
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
import kotlin.time.measureTime

class GenerateValidationReport(
    database: Database,
    private val avtaler: AvtaleRepository,
    private val avtaleValidator: AvtaleValidator,
    private val gjennomforinger: TiltaksgjennomforingRepository,
    private val gjennomforingValidator: TiltaksgjennomforingValidator,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.name)
        .execute { instance, context ->
            logger.info("Running task ${instance.taskName}")

            runBlocking {
                val job = async {
                    val report = createReport()
                    upload(report)
                }

                while (job.isActive) {
                    if (context.schedulerState.isShuttingDown) {
                        logger.info("Stopping task ${instance.taskName} due to shutdown signal")

                        job.cancelAndJoin()

                        logger.info("Task ${instance.taskName} stopped")
                    } else {
                        delay(1000)
                    }
                }
            }
        }

    private val client = SchedulerClient.Builder.create(database.getDatasource(), task).build()

    private val storage: Storage = StorageOptions.getDefaultInstance().service

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        client.schedule(task.instance(id.toString()), startTime)
        return id
    }

    private suspend fun upload(report: XSSFWorkbook) {
        if (NaisEnv.current().isLocal()) {
            writeToTempFile(report)
        } else {
            uploadToBucket(report)
        }
    }

    private suspend fun uploadToBucket(report: XSSFWorkbook) = withContext(Dispatchers.IO) {
        val bucketName = "mulighetsrommet-api-uploads"
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

    private suspend fun validateAvtaler() = buildMap {
        fanOut({ pagination -> avtaler.getAll(pagination).second }) {
            val dbo = toAvtaleDbo(it)
            avtaleValidator.validate(dbo)
                .onLeft { validationErrors ->
                    put(it, validationErrors)
                }
        }
    }

    private fun createAvtalerSheet(
        workbook: XSSFWorkbook,
        result: Map<AvtaleAdminDto, List<ValidationError>>,
    ) {
        val workSheet = workbook.createSheet("Avtaler")
        createHeader(workSheet)

        var rowNumber = 1
        result.forEach { (dto, errors) ->
            errors.forEach { error ->
                createRow(workSheet, rowNumber++, dto.id, dto.navn, dto.opphav.name, dto.avtalestatus.name, error)
            }
        }
    }

    private suspend fun validateGjennomforinger() = buildMap {
        fanOut({ pagination -> gjennomforinger.getAll(pagination).second }) {
            val dbo = toTiltaksgjennomforingDbo(it)
            gjennomforingValidator.validate(dbo)
                .onLeft { validationErrors ->
                    put(it, validationErrors)
                }
        }
    }

    private fun createGjennomforingerSheet(
        workbook: XSSFWorkbook,
        result: Map<TiltaksgjennomforingAdminDto, List<ValidationError>>,
    ) {
        val workSheet = workbook.createSheet("Gjennomføringer")
        createHeader(workSheet)

        var rowNumber = 1
        result.forEach { (dto, errors) ->
            errors.forEach { error ->
                createRow(workSheet, rowNumber++, dto.id, dto.navn, dto.opphav.name, dto.status.name, error)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> fanOut(
        producer: (PaginationParams) -> List<T>,
        consumer: suspend (T) -> Unit,
    ) = coroutineScope {
        val numConsumers = 10
        val channelCapacity = 1000

        var count = 0

        // Produce events in a separate coroutine
        val events = produce(capacity = channelCapacity) {
            val totalCount = paginateSuspend(channelCapacity) { pagination ->
                val items = producer(pagination)

                items.forEach {
                    send(it)
                }

                items
            }

            count = totalCount

            close()
        }

        val time = measureTime {
            // Create `numConsumers` coroutines to process the events simultaneously
            (0..numConsumers)
                .map {
                    async {
                        events.consumeEach { event ->
                            consumer.invoke(event)
                        }
                    }
                }
                .awaitAll()
        }

        logger.info("Consumed $count events in $time")
    }
}

private fun toAvtaleDbo(dto: AvtaleAdminDto) = dto.run {
    AvtaleDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstype.id,
        avtalenummer = avtalenummer,
        leverandorOrganisasjonsnummer = leverandor.organisasjonsnummer,
        leverandorUnderenheter = leverandorUnderenheter.map { it.organisasjonsnummer },
        leverandorKontaktpersonId = leverandorKontaktperson?.id,
        startDato = startDato,
        sluttDato = sluttDato,
        navRegion = navRegion?.enhetsnummer ?: "",
        navEnheter = navEnheter.map { it.enhetsnummer },
        avtaletype = avtaletype,
        opphav = opphav,
        prisbetingelser = prisbetingelser,
        antallPlasser = antallPlasser,
        url = url,
        administratorer = administrator?.let { listOf(it.navIdent) } ?: listOf(),
        updatedAt = updatedAt,
    )
}

private fun toTiltaksgjennomforingDbo(dto: TiltaksgjennomforingAdminDto) = dto.run {
    TiltaksgjennomforingDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = tiltaksnummer,
        arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer,
        arrangorKontaktpersonId = arrangor.kontaktperson?.id,
        startDato = startDato,
        sluttDato = sluttDato,
        avslutningsstatus = when (status) {
            Tiltaksgjennomforingsstatus.APENT_FOR_INNSOK, Tiltaksgjennomforingsstatus.GJENNOMFORES -> Avslutningsstatus.IKKE_AVSLUTTET
            Tiltaksgjennomforingsstatus.AVLYST -> Avslutningsstatus.AVLYST
            Tiltaksgjennomforingsstatus.AVBRUTT -> Avslutningsstatus.AVBRUTT
            Tiltaksgjennomforingsstatus.AVSLUTTET -> Avslutningsstatus.AVSLUTTET
        },
        tilgjengelighet = tilgjengelighet,
        estimertVentetid = estimertVentetid,
        antallPlasser = antallPlasser ?: -1,
        avtaleId = avtaleId ?: id,
        administratorer = administrator?.let { listOf(it.navIdent) } ?: listOf(),
        navEnheter = navEnheter.map { it.enhetsnummer },
        oppstart = oppstart,
        opphav = opphav,
        stengtFra = stengtFra,
        stengtTil = stengtTil,
        kontaktpersoner = kontaktpersoner.map {
            TiltaksgjennomforingKontaktpersonDbo(
                navIdent = it.navIdent,
                navEnheter = it.navEnheter,
            )
        },
        stedForGjennomforing = stedForGjennomforing,
        faneinnhold = faneinnhold,
        beskrivelse = beskrivelse,
    )
}
