package no.nav.mulighetsrommet.api.utbetaling.task

import arrow.core.Either
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.toArrFlateUtbetaling
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkError
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.Journalpost
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.utbetaling.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatus
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
    private val tilsagnService: TilsagnService,
    private val dokarkClient: DokarkClient,
    private val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
    private val pdf: PdfGenClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val utbetalingId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .executeSuspend { inst, _ ->
            journalfor(inst.data.utbetalingId)
        }

    fun schedule(utbetalingId: UUID, startTime: Instant, tx: TransactionalSession): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(utbetalingId))
        val client = transactionalSchedulerClient(task, tx.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalfor(id: UUID): Either<DokarkError, DokarkResponse> = db.session {
        logger.info("Journalfører utbetaling med id: $id")

        val utbetaling = requireNotNull(queries.utbetaling.get(id)) { "Fant ikke utbetaling med id=$id" }
            .also { require(it.status == UtbetalingStatus.GODKJENT_AV_ARRANGOR) { "utbetaling må være godkjent" } }

        val gjennomforing = queries.gjennomforing.get(utbetaling.gjennomforing.id)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til utbetaling med id=$id" }

        val fagsakId = gjennomforing.tiltaksnummer ?: gjennomforing.lopenummer

        val pdf = run {
            val tilsagn = tilsagnService.getArrangorflateTilsagnTilUtbetaling(
                gjennomforingId = utbetaling.gjennomforing.id,
                periode = utbetaling.periode,
            )
            val utbetalingAft = toArrFlateUtbetaling(db, pdl, utbetaling)
            pdf.utbetalingJournalpost(utbetalingAft, tilsagn)
        }

        val journalpost = utbetalingJournalpost(pdf, utbetaling.id, utbetaling.arrangor, fagsakId)

        dokarkClient.opprettJournalpost(journalpost, AccessType.M2M)
            .onRight {
                queries.utbetaling.setJournalpostId(id, it.journalpostId)
            }
            .onLeft {
                throw Exception("Feil ved opprettelse av journalpost. Message: ${it.message}")
            }
    }
}

fun utbetalingJournalpost(
    pdf: ByteArray,
    utbetalingId: UUID,
    arrangor: UtbetalingDto.Arrangor,
    fagsakId: String,
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
    ),
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
