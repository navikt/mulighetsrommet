package no.nav.mulighetsrommet.api.refusjon.task

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.arrangorflate.toRefusjonskrav
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.Journalpost
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class JournalforRefusjonskrav(
    private val refusjonskravRepository: RefusjonskravRepository,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val tilsagnService: TilsagnService,
    private val dokarkClient: DokarkClient,
    private val deltakerRepository: DeltakerRepository,
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

    suspend fun journalforRefusjonskrav(id: UUID) {
        logger.info("Journalfører refusjonskrav med id: $id")

        val krav = requireNotNull(refusjonskravRepository.get(id)) { "Fant ikke refusjonskrav med id=$id" }
            .also { require(it.status == RefusjonskravStatus.GODKJENT_AV_ARRANGOR) { "Krav må være godkjent" } }

        val gjennomforing = tiltaksgjennomforingRepository.get(krav.gjennomforing.id)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til refusjonskrav med id=$id" }
        val fagsakId = gjennomforing.tiltaksnummer ?: gjennomforing.lopenummer
        requireNotNull(fagsakId) { "FagsakId var null for gjennomføring med id=${gjennomforing.id}" }

        val pdf = run {
            val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                gjennomforingId = krav.gjennomforing.id,
                periode = krav.beregning.input.periode,
            )
            val refusjonsKravAft = toRefusjonskrav(pdl, deltakerRepository, krav)
            pdf.refusjonJournalpost(refusjonsKravAft, tilsagn)
        }

        val journalpost = refusjonskravJournalpost(pdf, krav.id, krav.arrangor, fagsakId)

        dokarkClient.opprettJournalpost(journalpost, AccessType.M2M)
            .onRight {
                refusjonskravRepository.setJournalpostId(id, it.journalpostId)
            }
            .onLeft {
                throw Exception("Feil ved opprettelse av journalpost. Message: ${it.message}")
            }
    }
}

fun refusjonskravJournalpost(
    pdf: ByteArray,
    refusjonskravId: UUID,
    arrangor: RefusjonskravDto.Arrangor,
    fagsakId: String,
): Journalpost = Journalpost(
    tittel = "Refusjonskrav",
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
    sak = Journalpost.Sak(
        sakstype = Journalpost.Sak.Sakstype.FAGSAK,
        fagsakId = fagsakId,
        fagsaksystem = Journalpost.Sak.Fagsaksystem.TILTAKSADMINISTRASJON,
    ),
    behandlingstema = null,
)
