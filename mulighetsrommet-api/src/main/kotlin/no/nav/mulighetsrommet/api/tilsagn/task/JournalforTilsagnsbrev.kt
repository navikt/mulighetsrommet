package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakerPersonalia
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.Journalpost
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.JournalpostId
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class JournalforTilsagnsbrev(
    private val db: ApiDatabase,
    private val dokarkClient: DokarkClient,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val pdf: PdfGenClient,
    private val distribuerTilsagnsbrev: DistribuerTilsagnsbrev,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler<TaskData>(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            journalfor(inst.data.tilsagnId).onLeft { message ->
                throw Exception("Feil ved journalføring av tilsagnsbrev med id=${inst.data.tilsagnId}: $message")
            }.map { journalpostId ->
                logger.info("Skedulerer distribusjon av tilsagnsbrev journapostId: $journalpostId, tilsagnId: ${inst.data.tilsagnId}")
                distribuerTilsagnsbrev.schedule(inst.data.tilsagnId)
            }
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(tilsagnId: UUID, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(tilsagnId))
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalfor(tilsagnId: UUID): Either<String, JournalpostId> = db.transaction {
        logger.info("Journalfører tilsagn med id: $tilsagnId")

        val tilsagn = queries.tilsagn.getOrError(tilsagnId)
        if (tilsagn.journalpost != null) {
            logger.info("Tilsagn med id $tilsagnId har allerede journalpostId ${tilsagn.journalpost.id}, hopper journalføring")
            return@transaction Either.Right(tilsagn.journalpost.id)
        }
        val enkeltplass = queries.gjennomforing.getGjennomforingEnkeltplassOrError(tilsagn.gjennomforing.id)
        val deltaker = queries.deltaker.getByGjennomforingId(enkeltplass.id).single()
        val personalia = amtDeltakerClient.hentPersonalia(setOf(deltaker.id))
            .getOrElse {
                return@transaction Either.Left("Kunne ikke hente personalia fra amt-deltaker med id: $it")
            }.single()
        val arrangor = queries.arrangor.get(tilsagn.arrangor.organisasjonsnummer)
            ?: return@transaction Either.Left("Fant ikke arrangør med organisasjonsnummer ${tilsagn.arrangor.organisasjonsnummer} for tilsagn med id $tilsagnId")

        val fagsakId = enkeltplass.arena?.tiltaksnummer?.value ?: enkeltplass.lopenummer.value

        val journalpostResult = generatePdf(tilsagn, personalia)
            .flatMap { pdf ->
                val journalpost = tilsagnJournalpost(
                    pdf = pdf,
                    tilsagnId = tilsagn.id,
                    deltaker = personalia.norskIdent,
                    arrangor = arrangor,
                    fagsakId = fagsakId,
                )
                dokarkClient
                    .opprettJournalpost(journalpost, AccessType.M2M)
                    .mapLeft { error -> "Feil fra dokark ved journalføring av tilsagn $tilsagnId: ${error.message}" }
            }

        journalpostResult.map { response ->
            queries.tilsagn.setJournalpostId(tilsagnId, response.journalpostId)
            if (!response.journalpostferdigstilt) {
                logger.info("Journalpost ${response.journalpostId} for tilsagn $tilsagnId er ikke ferdigstilt: ${response.melding}")
            }
            response.journalpostId
        }
    }

    private suspend fun generatePdf(tilsagn: Tilsagn, deltaker: DeltakerPersonalia): Either<String, ByteArray> {
        val content = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
            tilsagn,
            deltaker,
        )
        return pdf
            .getPdfDocument(content)
            .mapLeft { error -> "Feil ved generering av tilsagnsbrev. pdfgen: $error" }
    }
}

fun tilsagnJournalpost(
    pdf: ByteArray,
    tilsagnId: UUID,
    deltaker: NorskIdent,
    arrangor: ArrangorDto,
    fagsakId: String,
): Journalpost = Journalpost(
    tittel = "Tilsagnsbrev",
    journalposttype = "UTGAAENDE",
    avsenderMottaker = Journalpost.AvsenderMottaker(
        id = arrangor.organisasjonsnummer.value,
        idType = "ORGNR",
        navn = arrangor.navn,
    ),
    bruker = Journalpost.Bruker(
        id = deltaker.value,
        idType = "FNR",
    ),
    tema = "TIL", // Tiltak
    kanal = "ALTINN", // https://confluence.adeo.no/spaces/BOA/pages/316407153/Utsendingskanal
    journalfoerendeEnhet = "9999", // Automatisk journalføring
    eksternReferanseId = tilsagnId.toString(),
    datoMottatt = LocalDateTime.now().toString(),
    dokumenter = listOf(
        Journalpost.Dokument(
            tittel = "Tilsagn",
            dokumentvarianter = listOf(
                Journalpost.Dokument.Dokumentvariant(
                    "PDFA",
                    pdf,
                    "ARKIV",
                ),
            ),
        ),
    ),
    sak = Journalpost.Sak(
        sakstype = Journalpost.Sak.Sakstype.FAGSAK,
        fagsakId = fagsakId,
        fagsaksystem = Journalpost.Sak.Fagsaksystem.TILTAKSADMINISTRASJON,
    ),
    behandlingstema = null,
)
