package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.Either
import arrow.core.flatMap
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.Journalpost
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.JournalpostId
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
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

class JournalforEnkeltplassTilsagnsbrev(
    private val db: ApiDatabase,
    private val dokarkClient: DokarkClient,
    private val personaliaService: PersonaliaService,
    private val pdf: PdfGenClient,
    private val sendTilsagnsbrevTilAltinn: SendTilsagnsbrevTilAltinn,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
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
            }.onRight { journalpostId ->
                logger.info("Skedulerer sending av tilsagnsbrev til Altinn journalpostId: $journalpostId, tilsagnId: ${inst.data.tilsagnId}")
                sendTilsagnsbrevTilAltinn.schedule(inst.data.tilsagnId)
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

        val eksisterendeJournalpost = queries.tilsagn.getOrError(tilsagnId).journalpost
        if (eksisterendeJournalpost != null) {
            logger.info("Tilsagn med id $tilsagnId er allrede journalført med id ${eksisterendeJournalpost.id}")
            return@transaction Either.Right(eksisterendeJournalpost.id)
        }

        hentTilsagnsbrevInnhold(tilsagnId, personaliaService, kontoregisterOrganisasjonClient)
            .flatMap { innhold ->
                generatePdf(innhold).flatMap { pdf ->
                    val journalpost = tilsagnJournalpost(
                        pdf = pdf,
                        tilsagnId = innhold.tilsagn.id,
                        deltaker = requireNotNull(innhold.personalia.norskIdent()),
                        arrangor = innhold.arrangor,
                        fagsakId = innhold.fagsakId,
                    )
                    dokarkClient
                        .opprettJournalpost(journalpost, AccessType.M2M)
                        .mapLeft { error -> "Feil fra dokark ved journalføring av tilsagn $tilsagnId: ${error.message}" }
                }
            }
            .map { response ->
                queries.tilsagn.setJournalpostId(tilsagnId, response.journalpostId)
                if (!response.journalpostferdigstilt) {
                    logger.info("Journalpost ${response.journalpostId} for tilsagn $tilsagnId er ikke ferdigstilt: ${response.melding}")
                }
                response.journalpostId
            }
    }

    private suspend fun generatePdf(innhold: TilsagnsbrevInnhold): Either<String, ByteArray> {
        val content = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
            innhold.tilsagn,
            innhold.kontonummer,
            innhold.personalia,
            saksbehandler = innhold.saksbehandler,
            beslutter = innhold.beslutter,
            visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = false,
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
    arrangor: Arrangor,
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
    kanal = "INGEN_DISTRIBUSJON", // https://confluence.adeo.no/spaces/BOA/pages/316407153/Utsendingskanal
    journalfoerendeEnhet = "9999", // Automatisk journalføring
    eksternReferanseId = tilsagnId.toString(),
    datoMottatt = LocalDateTime.now().toString(),
    dokumenter = listOf(
        Journalpost.Dokument(
            tittel = "Tilsagnsbrev",
            brevKode = "Tilsagnsbrev_v1",
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
)
