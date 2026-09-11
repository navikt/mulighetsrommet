package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.Either
import arrow.core.flatMap
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnTilAltinnMapper
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.slf4j.LoggerFactory
import java.time.Duration.ofMinutes
import java.time.Instant
import java.util.UUID

/**
 * Sender tilsagnsbrev til arrangøren via Altinns Correspondence-API, se https://docs.altinn.studio/correspondence/.
 *
 * I motsetning til dokumentet som journalføres i Joark, som ikke skal inneholde personopplysninger om
 * adressebeskyttede/skjermede deltakere, skal dokumentet som sendes til arrangøren vise navn og fødselsnummer.
 */
class SendTilsagnsbrevTilAltinn(
    private val db: ApiDatabase,
    private val altinnCorrespondenceClient: AltinnCorrespondenceClient,
    private val personaliaService: PersonaliaService,
    private val pdf: PdfGenClient,
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
            sendTilAltinn(inst.data.tilsagnId).onLeft { message ->
                throw Exception("Feil ved sending av tilsagnsbrev til Altinn for tilsagn id=${inst.data.tilsagnId}: $message")
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

    suspend fun sendTilAltinn(tilsagnId: UUID): Either<String, UUID> = db.transaction {
        logger.info("Sender tilsagnsbrev til Altinn for tilsagn med id: $tilsagnId")

        val eksisterendeTilsagn = queries.tilsagn.getOrError(tilsagnId)
        val eksisterendeCorrespondenceId = eksisterendeTilsagn.journalpost?.altinnCorrespondenceId
        if (eksisterendeCorrespondenceId != null) {
            logger.info("Tilsagn med id $tilsagnId er allerede sendt til Altinn med referanse $eksisterendeCorrespondenceId")
            return@transaction Either.Right(UUID.fromString(eksisterendeCorrespondenceId))
        }
        require(eksisterendeTilsagn.journalpost?.id != null) {
            "Tilsagn med id=$tilsagnId har ingen journalpostId, kan ikke sende tilsagnsbrev til Altinn"
        }

        hentTilsagnsbrevInnhold(tilsagnId, personaliaService, kontoregisterOrganisasjonClient)
            .flatMap { innhold -> generatePdf(innhold).map { pdf -> innhold to pdf } }
            .flatMap { (innhold, pdf) ->
                val vedlegg = TilsagnTilAltinnMapper.tilAltinnVedlegg(innhold.tilsagn, innhold.personalia, pdf)
                altinnCorrespondenceClient.sendVedlegg(vedlegg, pdf)
                    .mapLeft { error -> "Feil ved opplasting av vedlegg til Altinn for tilsagn $tilsagnId: ${error.message}" }
                    .map { vedleggId -> innhold to vedleggId }
            }
            .flatMap { (innhold, vedleggId) ->
                val korrespondanse = TilsagnTilAltinnMapper.tilAltinnKorrespondanse(
                    tilsagn = innhold.tilsagn,
                    personalia = innhold.personalia,
                    vedleggId = vedleggId,
                    // Deterministisk idempotent-nøkkel basert på tilsagnId, slik at reforsøk av
                    // tasken ikke fører til at samme tilsagnsbrev sendes flere ganger til Altinn.
                    idempotentKey = tilsagnId,
                )
                altinnCorrespondenceClient.sendKorrespondanse(korrespondanse)
                    .mapLeft { error -> "Feil ved sending av korrespondanse til Altinn for tilsagn $tilsagnId: ${error.message}" }
            }
            .map { correspondenceId ->
                queries.tilsagn.setAltinnCorrespondenceId(tilsagnId, correspondenceId.toString())
                logger.info("Tilsagnsbrev for tilsagn $tilsagnId sendt til Altinn med referanse $correspondenceId")
                correspondenceId
            }
    }

    private suspend fun generatePdf(innhold: TilsagnsbrevInnhold): Either<String, ByteArray> {
        val content = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
            innhold.tilsagn,
            innhold.kontonummer,
            innhold.personalia,
            saksbehandler = innhold.saksbehandler,
            beslutter = innhold.beslutter,
            visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = true,
        )
        return pdf
            .getPdfDocument(content)
            .mapLeft { error -> "Feil ved generering av tilsagnsbrev. pdfgen: $error" }
    }
}
