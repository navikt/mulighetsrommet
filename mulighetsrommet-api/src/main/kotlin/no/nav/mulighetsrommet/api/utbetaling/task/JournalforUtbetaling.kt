package no.nav.mulighetsrommet.api.utbetaling.task

import arrow.core.Either
import arrow.core.flatMap
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.Journalpost
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.utbetaling.mapper.UbetalingToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.clamav.Vedlegg
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class JournalforUtbetaling(
    private val db: ApiDatabase,
    private val dokarkClient: DokarkClient,
    private val arrangorFlateService: ArrangorFlateService,
    private val pdf: PdfGenClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val utbetalingId: UUID,
        val vedlegg: List<Vedlegg>,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .executeSuspend { inst, _ ->
            journalfor(inst.data.utbetalingId, inst.data.vedlegg).onLeft { message ->
                throw Exception("Feil ved journalføring av utbetaling med id=${inst.data.utbetalingId}: $message")
            }
        }

    fun schedule(utbetalingId: UUID, startTime: Instant, tx: TransactionalSession, vedlegg: List<Vedlegg>): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(utbetalingId, vedlegg))
        val client = transactionalSchedulerClient(task, tx.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalfor(id: UUID, vedlegg: List<Vedlegg>): Either<String, DokarkResponse> = db.session {
        logger.info("Journalfører utbetaling med id: $id")

        val utbetaling = requireNotNull(queries.utbetaling.get(id)) { "Fant ikke utbetaling med id=$id" }

        val gjennomforing = queries.gjennomforing.get(utbetaling.gjennomforing.id)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til utbetaling med id=$id" }

        val fagsakId = gjennomforing.tiltaksnummer ?: gjennomforing.lopenummer

        generatePdf(utbetaling)
            .flatMap { pdf ->
                val journalpost = utbetalingJournalpost(pdf, utbetaling.id, utbetaling.arrangor, fagsakId, vedlegg)
                dokarkClient
                    .opprettJournalpost(journalpost, AccessType.M2M)
                    .mapLeft { error -> "Feil fra dokark: ${error.message}" }
            }
            .onRight {
                queries.utbetaling.setJournalpostId(id, it.journalpostId)
            }
    }

    private suspend fun generatePdf(utbetaling: Utbetaling): Either<String, ByteArray> {
        val arrflateUtbetaling = arrangorFlateService.toArrFlateUtbetaling(utbetaling)
        val content = UbetalingToPdfDocumentContentMapper.toJournalpostPdfContent(arrflateUtbetaling)
        return pdf
            .getPdfDocument(content)
            .mapLeft { error -> "Feil fra pdfgen: $error" }
    }
}

fun utbetalingJournalpost(
    pdf: ByteArray,
    utbetalingId: UUID,
    arrangor: Utbetaling.Arrangor,
    fagsakId: String,
    vedlegg: List<Vedlegg>,
): Journalpost = Journalpost(
    tittel = "Utbetaling",
    journalposttype = "INNGAAENDE",
    avsenderMottaker = Journalpost.AvsenderMottaker(
        id = arrangor.organisasjonsnummer.value,
        idType = "ORGNR",
        navn = arrangor.navn,
    ),
    bruker = Journalpost.Bruker(
        id = arrangor.organisasjonsnummer.value,
        idType = "ORGNR",
    ),
    tema = "TIL",
    datoMottatt = LocalDateTime.now().toString(),
    dokumenter = listOf(
        Journalpost.Dokument(
            tittel = "Utbetaling",
            dokumentvarianter = listOf(
                Journalpost.Dokument.Dokumentvariant(
                    "PDFA",
                    pdf,
                    "ARKIV",
                ),
            ),
        ),
    ) + vedlegg.map {
        Journalpost.Dokument(
            tittel = it.filename,
            dokumentvarianter = listOf(
                Journalpost.Dokument.Dokumentvariant(
                    "PDF",
                    it.content.content,
                    "ARKIV",
                ),
            ),
        )
    },
    eksternReferanseId = utbetalingId.toString(),
    journalfoerendeEnhet = "9999", // Automatisk journalføring
    kanal = "NAV_NO", // Påkrevd for INNGAENDE. Se https://confluence.adeo.no/display/BOA/Mottakskanal
    sak = Journalpost.Sak(
        sakstype = Journalpost.Sak.Sakstype.FAGSAK,
        fagsakId = fagsakId,
        fagsaksystem = Journalpost.Sak.Fagsaksystem.TILTAKSADMINISTRASJON,
    ),
    behandlingstema = null,
)
