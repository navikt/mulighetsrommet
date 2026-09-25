package no.nav.mulighetsrommet.api.tilskuddbehandling.task

import arrow.core.Either
import arrow.core.flatMap
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.Journalpost
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilskuddbehandling.mapper.TilskuddVedtakToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class JournalforVedtaksbrev(
    private val db: ApiDatabase,
    private val dokarkClient: DokarkClient,
    private val personaliaService: PersonaliaService,
    private val pdf: PdfGenClient,
    private val distribuerVedtaksbrev: DistribuerVedtaksbrev,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val vedtakId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler<TaskData>(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            journalfor(inst.data.vedtakId).onLeft { message ->
                throw Exception("Feil ved journalføring av vedtak med id=${inst.data.vedtakId}: $message")
            }.onRight { response ->
                logger.info("Skedulerer distribusjon av vedtaksbrev journalpostId: $response, vedtakId: ${inst.data.vedtakId}")
                distribuerVedtaksbrev.schedule(inst.data.vedtakId)
            }
        }

    fun schedule(vedtakId: UUID, startTime: Instant, tx: TransactionalSession): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(vedtakId))
        val client = transactionalSchedulerClient(task, tx.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalfor(id: UUID): Either<String, String> = db.transaction {
        val vedtakJournalpostId = queries.tilskuddBehandling.getVedtakJournalpostId(id)
        if (vedtakJournalpostId != null) {
            logger.info("Vedtak om tilskudd er allerede journalført med id $vedtakJournalpostId")
            return@transaction Either.Right(vedtakJournalpostId)
        }

        logger.info("Journalfører vedtak med id: $id")

        hentVedtaksbrevInnhold(id, personaliaService).flatMap { innhold ->
            generatePdf(innhold)
                .flatMap { pdf ->
                    val journalpost = vedtakJournalpost(
                        pdf,
                        id,
                        innhold.deltakerPersonalia.norskIdent,
                        innhold.tiltak.lopenummer,
                    )
                    dokarkClient
                        .opprettJournalpost(journalpost, AccessType.M2M)
                        .mapLeft { error -> "Feil fra dokark: ${error.message}" }
                }
                .map { response ->
                    queries.tilskuddBehandling.setJournalpostId(id, response.journalpostId)
                    response.journalpostId
                }
        }
    }

    private suspend fun generatePdf(
        innhold: VedtaksbrevInnhold,
    ): Either<String, ByteArray> {
        val content = TilskuddVedtakToPdfDocumentContentMapper.toPdfDocumentContent(
            innhold,
        )
        return pdf
            .getPdfDocument(content)
            .mapLeft { error ->
                "Feil fra pdfgen: ${error.detail}"
            }
    }
}

fun vedtakJournalpost(
    pdf: ByteArray,
    vedtakId: UUID,
    fnr: String,
    fagsakId: String,
): Journalpost = Journalpost(
    tittel = "Vedtak om tilskudd til opplæring",
    journalposttype = "UTGAAENDE",
    avsenderMottaker = Journalpost.AvsenderMottaker(
        id = fnr,
        idType = "FNR",
        navn = null,
    ),
    bruker = Journalpost.Bruker(
        id = fnr,
        idType = "FNR",
    ),
    tema = "TIL",
    datoMottatt = LocalDateTime.now().toString(),
    dokumenter = listOf(
        Journalpost.Dokument(
            tittel = "Vedtak om tilskudd til opplæring",
            brevKode = "tilskudd-vedtak",
            dokumentvarianter = listOf(
                Journalpost.Dokument.Dokumentvariant(
                    "PDFA",
                    pdf,
                    "ARKIV",
                ),
            ),
        ),
    ),
    eksternReferanseId = vedtakId.toString(),
    journalfoerendeEnhet = "9999", // Automatisk journalføring,
    sak = Journalpost.Sak(
        sakstype = Journalpost.Sak.Sakstype.FAGSAK,
        fagsakId = fagsakId,
        fagsaksystem = Journalpost.Sak.Fagsaksystem.TILTAKSADMINISTRASJON,
    ),
    kanal = "NAV_NO",
)
