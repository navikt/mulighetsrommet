package no.nav.mulighetsrommet.api.refusjon.task

import arrow.core.Either
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.toRefusjonskrav
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkError
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.Journalpost
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class JournalforRefusjonskrav(
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
        val refusjonskravId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .executeSuspend { inst, _ ->
            // TODO: Midlertidig avskrudd i påvente av endringer
            // Vi skal få fagsystem hos dokarkiv
            // journalforRefusjonskrav(inst.data.refusjonskravId)
        }

    fun schedule(refusjonskravId: UUID, startTime: Instant, tx: TransactionalSession): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(refusjonskravId))
        val client = transactionalSchedulerClient(task, tx.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalforRefusjonskrav(id: UUID): Either<DokarkError, DokarkResponse> = db.session {
        logger.info("Journalfører refusjonskrav med id: $id")

        val krav = requireNotNull(Queries.refusjonskrav.get(id)) { "Fant ikke refusjonskrav med id=$id" }
            .also { require(it.status == RefusjonskravStatus.GODKJENT_AV_ARRANGOR) { "Krav må være godkjent" } }

        val pdf = run {
            val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                gjennomforingId = krav.gjennomforing.id,
                periode = krav.beregning.input.periode,
            )
            val refusjonsKravAft = toRefusjonskrav(db, pdl, krav)
            pdf.refusjonJournalpost(refusjonsKravAft, tilsagn)
        }

        val journalpost = refusjonskravJournalpost(pdf, krav.id, krav.arrangor.organisasjonsnummer)

        dokarkClient.opprettJournalpost(journalpost, AccessType.M2M)
            .onRight {
                Queries.refusjonskrav.setJournalpostId(id, it.journalpostId)
            }
            .onLeft {
                throw Exception("Feil ved opprettelse av journalpost. Message: ${it.message}")
            }
    }
}

fun refusjonskravJournalpost(
    pdf: ByteArray,
    refusjonskravId: UUID,
    organisasjonsnummer: Organisasjonsnummer,
): Journalpost = Journalpost(
    tittel = "Refusjonskrav",
    journalposttype = "INNGAAENDE",
    avsenderMottaker = Journalpost.AvsenderMottaker(
        id = organisasjonsnummer.value,
        idType = "ORGNR",
        navn = null,
    ),
    bruker = Journalpost.Bruker(
        id = organisasjonsnummer.value,
        idType = "ORGNR",
    ),
    tema = "TIL",
    datoMottatt = LocalDateTime.now().toString(),
    dokumenter = listOf(
        Journalpost.Dokument(
            tittel = "Refusjonskrav",
            dokumentvarianter = listOf(
                Journalpost.Dokument.Dokumentvariant(
                    "PDFA",
                    pdf,
                    "ARKIV",
                ),
            ),
        ),
    ),
    eksternReferanseId = refusjonskravId.toString(),
    journalfoerendeEnhet = "9999", // Automatisk journalføring
    kanal = "NAV_NO", // Påkrevd for INNGAENDE. Se https://confluence.adeo.no/display/BOA/Mottakskanal
    sak = null,
    behandlingstema = null,
)
