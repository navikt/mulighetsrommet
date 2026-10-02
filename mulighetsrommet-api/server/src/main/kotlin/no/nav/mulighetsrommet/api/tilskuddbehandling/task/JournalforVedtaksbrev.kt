package no.nav.mulighetsrommet.api.tilskuddbehandling.task

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
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
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
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
        val behandlingId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler<TaskData>(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            journalfor(inst.data.behandlingId).onLeft { message ->
                throw Exception("Feil ved journalføring av tilskuddsbehandling med id=${inst.data.behandlingId}: $message")
            }.onRight { resultat ->
                when (resultat) {
                    is JournalForResultat.Success -> {
                        logger.info("Skedulerer distribusjon av vedtaksbrev journalpostId: ${resultat.vedtakJournalpostId}, behandlingId: ${inst.data.behandlingId}")
                        distribuerVedtaksbrev.schedule(inst.data.behandlingId)
                    }

                    is JournalForResultat.Noop -> {
                        logger.info(resultat.message)
                    }
                }
            }
        }

    fun schedule(behandlingId: UUID, startTime: Instant, tx: TransactionalSession) {
        val instance = task.instance(behandlingId.toString(), TaskData(behandlingId))
        val client = transactionalSchedulerClient(task, tx.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
    }

    suspend fun journalfor(behandlingId: UUID): Either<String, JournalForResultat> = db.transaction {
        val behandling = queries.tilskuddBehandling.getOrError(behandlingId)
        if (behandling.type == TilskuddBehandlingType.REVURDERING) {
            // TODO: Noop burde fjernes når en implementerer opphør og/eller revurdering
            // Brukes til testing av opphør
            return@transaction JournalForResultat.Noop("Revurdering behandling er ikke implementert, journalføres ikke. BehandlingId: $behandlingId").right()
        }

        val vedtakJournalpostId = queries.tilskuddBehandling.getVedtakJournalpostId(behandlingId)
        if (vedtakJournalpostId != null) {
            logger.info("Vedtak om tilskudd er allerede journalført med id $vedtakJournalpostId")
            return@transaction JournalForResultat.Success(vedtakJournalpostId).right()
        }

        logger.info("Journalfører vedtak med id: $behandlingId")

        hentVedtaksbrevInnhold(behandlingId, personaliaService).flatMap { innhold ->
            generatePdf(innhold)
                .flatMap { pdf ->
                    val journalpost = vedtakJournalpost(
                        pdf,
                        behandlingId,
                        innhold.deltakerPersonalia.norskIdent,
                        innhold.tiltak.lopenummer,
                    )
                    dokarkClient
                        .opprettJournalpost(journalpost, AccessType.M2M)
                        .mapLeft { error -> "Feil fra dokark: ${error.message}" }
                }
                .map { response ->
                    queries.tilskuddBehandling.setJournalpostId(behandlingId, response.journalpostId)
                    JournalForResultat.Success(response.journalpostId)
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
    behandlingId: UUID,
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
    eksternReferanseId = behandlingId.toString(),
    journalfoerendeEnhet = "9999", // Automatisk journalføring,
    sak = Journalpost.Sak(
        sakstype = Journalpost.Sak.Sakstype.FAGSAK,
        fagsakId = fagsakId,
        fagsaksystem = Journalpost.Sak.Fagsaksystem.TILTAKSADMINISTRASJON,
    ),
    kanal = "NAV_NO",
)

sealed interface JournalForResultat {
    data class Success(val vedtakJournalpostId: String) : JournalForResultat

    // TODO: Brukes midlertidig til håndtering av opphørsbehandling av tilskudd
    // Bør fjernes når man vurderer implementasjon av opphør eller revurdering av tilskudd
    data class Noop(val message: String) : JournalForResultat
}
