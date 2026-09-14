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
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokarkClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.Journalpost
import no.nav.mulighetsrommet.api.pdfgen.PdfGenClient
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnAltinnSnapshot
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnTilAltinnMapper
import no.nav.mulighetsrommet.api.tilsagn.mapper.TilsagnToPdfDocumentContentMapper
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

/**
 * Saga som håndterer opprettelse og distribusjon av tilsagnsbrev for enkeltplass-tilsagn.
 *
 * Kjøres i tre steg, hver representert ved en egen [OneTimeTask]:
 *
 * 1. [opprettInnhold] henter [TilsagnsbrevInnhold] og genererer to PDF-varianter fra _samme_ innhold:
 *    én sladdet (for Joark) og én usladdet (for Altinn). Alt de neste stegene trenger sendes videre
 *    som task-input for å unngå potensielle avvik mellom det arkiverte og det utsendte brevet.
 * 2. [arkiverIDokark] journalfører den sladdede PDF-en i Joark.
 * 3. [sendTilAltinn] sender den usladdede PDF-en til arrangøren.
 */
class SendTilsagnsbrevSaga(
    private val db: ApiDatabase,
    private val dokarkClient: DokarkClient,
    private val personaliaService: PersonaliaService,
    private val pdf: PdfGenClient,
    private val altinnCorrespondenceClient: AltinnCorrespondenceClient,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class OpprettInnholdTaskData(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
    )

    @Serializable
    data class ArkiverIDokarkTaskData(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        val pdfBase64: String,
        val deltakerFnr: String,
        val arrangorOrganisasjonsnummer: String,
        val arrangorNavn: String,
        val fagsakId: String,
    )

    @Serializable
    data class SendTilAltinnTaskData(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        val pdfBase64: String,
        val tiltakstypeNavn: String,
        val bestillingsnummer: String,
        val arrangorOrganisasjonsnummer: String,
        val arrangorNavn: String,
    )

    val opprettInnholdTask: OneTimeTask<OpprettInnholdTaskData> = Tasks
        .oneTime("SendTilsagnsbrevSaga-OpprettInnhold", OpprettInnholdTaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            opprettInnhold(inst.data.tilsagnId).onLeft { message ->
                throw Exception("Feil ved opprettelse av tilsagnsbrev for tilsagn med id=${inst.data.tilsagnId}: $message")
            }
        }

    val arkiverIDokarkTask: OneTimeTask<ArkiverIDokarkTaskData> = Tasks
        .oneTime("SendTilsagnsbrevSaga-ArkiverIDokark", ArkiverIDokarkTaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            arkiverIDokark(inst.data).onLeft { message ->
                throw Exception("Feil ved arkivering av tilsagnsbrev i Joark for tilsagn med id=${inst.data.tilsagnId}: $message")
            }
        }

    val sendTilAltinnTask: OneTimeTask<SendTilAltinnTaskData> = Tasks
        .oneTime("SendTilsagnsbrevSaga-SendTilAltinn", SendTilAltinnTaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            sendTilAltinn(inst.data).onLeft { message ->
                throw Exception("Feil ved sending av tilsagnsbrev til Altinn for tilsagn med id=${inst.data.tilsagnId}: $message")
            }
        }

    private val opprettInnholdClient = SchedulerClient.Builder
        .create(db.getDatasource(), opprettInnholdTask)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(tilsagnId: UUID, startTime: Instant = Instant.now()): UUID {
        val instance = opprettInnholdTask.instance(tilsagnId.toString(), OpprettInnholdTaskData(tilsagnId))
        opprettInnholdClient.scheduleIfNotExists(instance, startTime)
        return tilsagnId
    }

    suspend fun opprettInnhold(tilsagnId: UUID): Either<String, Unit> = db.transaction {
        logger.info("Oppretter tilsagnsbrev for tilsagn med id: $tilsagnId")

        val eksisterendeTilsagn = queries.tilsagn.getAndAcquireLock(tilsagnId)
        if (eksisterendeTilsagn.tilsagnsbrev?.journalpostId != null && eksisterendeTilsagn.tilsagnsbrev.altinnCorrespondenceId != null) {
            logger.info("Tilsagn med id $tilsagnId er allerede journalført og sendt til Altinn")
            return@transaction Either.Right(Unit)
        }

        hentTilsagnsbrevInnhold(tilsagnId, personaliaService, kontoregisterOrganisasjonClient).flatMap { innhold ->
            val deltakerFnr = innhold.personalia.norskIdent()
            if (deltakerFnr == null) {
                return@flatMap Either.Left("Har ikke tilgang til deltakers norske identitetsnummer for tilsagn $tilsagnId")
            }

            generatePdf(innhold, visPersonopplysninger = false).flatMap { pdfJoark ->
                generatePdf(innhold, visPersonopplysninger = true).map { pdfAltinn ->
                    Triple(deltakerFnr, pdfJoark, pdfAltinn)
                }
            }.map { (fnr, pdfJoark, pdfAltinn) ->
                val client = transactionalSchedulerClient(arkiverIDokarkTask, session.connection.underlying)
                client.scheduleIfNotExists(
                    arkiverIDokarkTask.instance(
                        tilsagnId.toString(),
                        ArkiverIDokarkTaskData(
                            tilsagnId = tilsagnId,
                            pdfBase64 = Base64.getEncoder().encodeToString(pdfJoark),
                            deltakerFnr = fnr.value,
                            arrangorOrganisasjonsnummer = innhold.arrangor.organisasjonsnummer.value,
                            arrangorNavn = innhold.arrangor.navn,
                            fagsakId = innhold.fagsakId,
                        ),
                    ),
                    Instant.now(),
                )

                val altinnClient = transactionalSchedulerClient(sendTilAltinnTask, session.connection.underlying)
                altinnClient.scheduleIfNotExists(
                    sendTilAltinnTask.instance(
                        tilsagnId.toString(),
                        SendTilAltinnTaskData(
                            tilsagnId = tilsagnId,
                            pdfBase64 = Base64.getEncoder().encodeToString(pdfAltinn),
                            tiltakstypeNavn = innhold.tilsagn.tiltakstype.navn,
                            bestillingsnummer = innhold.tilsagn.bestilling.bestillingsnummer,
                            arrangorOrganisasjonsnummer = innhold.arrangor.organisasjonsnummer.value,
                            arrangorNavn = innhold.arrangor.navn,
                        ),
                    ),
                    Instant.now(),
                )

                logger.info("Skedulerer arkivering i Joark og sending til Altinn for tilsagn med id: $tilsagnId")
            }
        }
    }

    private suspend fun generatePdf(innhold: TilsagnsbrevInnhold, visPersonopplysninger: Boolean): Either<String, ByteArray> {
        val content = TilsagnToPdfDocumentContentMapper.toTilsagnsbrev(
            innhold.tilsagn,
            innhold.kontonummer,
            innhold.personalia,
            saksbehandler = innhold.saksbehandler,
            beslutter = innhold.beslutter,
            visPersonopplysningerOmAdressebeskyttetEllerSkjermetPerson = visPersonopplysninger,
        )
        return pdf
            .getPdfDocument(content)
            .mapLeft { error -> "Feil ved generering av tilsagnsbrev. pdfgen: $error" }
    }

    suspend fun arkiverIDokark(data: ArkiverIDokarkTaskData): Either<String, Unit> = db.transaction {
        val tilsagnId = data.tilsagnId
        val eksisterendeTilsagn = queries.tilsagn.getOrError(tilsagnId)
        if (eksisterendeTilsagn.tilsagnsbrev?.journalpostId != null) {
            logger.info("Tilsagn med id $tilsagnId er allerede journalført med id ${eksisterendeTilsagn.tilsagnsbrev.journalpostId}")
            return@transaction Either.Right(Unit)
        }

        val journalpost = tilsagnJournalpost(
            pdf = Base64.getDecoder().decode(data.pdfBase64),
            tilsagnId = tilsagnId,
            deltaker = NorskIdent(data.deltakerFnr),
            arrangorOrganisasjonsnummer = Organisasjonsnummer(data.arrangorOrganisasjonsnummer),
            arrangorNavn = data.arrangorNavn,
            fagsakId = data.fagsakId,
        )

        dokarkClient
            .opprettJournalpost(journalpost, AccessType.M2M)
            .mapLeft { error -> "Feil fra dokark ved journalføring av tilsagn $tilsagnId: ${error.message}" }
            .map { response ->
                queries.tilsagn.setJournalpostId(tilsagnId, response.journalpostId)
                if (!response.journalpostferdigstilt) {
                    logger.info("Journalpost ${response.journalpostId} for tilsagn $tilsagnId er ikke ferdigstilt: ${response.melding}")
                }
            }
    }

    suspend fun sendTilAltinn(data: SendTilAltinnTaskData): Either<String, Unit> = db.transaction {
        val tilsagnId = data.tilsagnId
        val eksisterendeTilsagn = queries.tilsagn.getOrError(tilsagnId)
        if (eksisterendeTilsagn.tilsagnsbrev?.altinnCorrespondenceId != null) {
            logger.info("Tilsagn med id $tilsagnId er allerede sendt til Altinn med referanse ${eksisterendeTilsagn.tilsagnsbrev.altinnCorrespondenceId}")
            return@transaction Either.Right(Unit)
        }

        val pdfAltinn = Base64.getDecoder().decode(data.pdfBase64)
        val tilsagn = TilsagnAltinnSnapshot(
            tiltakstypeNavn = data.tiltakstypeNavn,
            bestillingsnummer = data.bestillingsnummer,
            arrangorOrganisasjonsnummer = Organisasjonsnummer(data.arrangorOrganisasjonsnummer),
            arrangorNavn = data.arrangorNavn,
        )

        val vedlegg = TilsagnTilAltinnMapper.tilAltinnVedlegg(tilsagn, pdfAltinn)
        altinnCorrespondenceClient.sendVedlegg(vedlegg, pdfAltinn)
            .mapLeft { error -> "Feil ved opplasting av vedlegg til Altinn for tilsagn $tilsagnId: ${error.message}" }
            .flatMap { vedleggId ->
                val korrespondanse = TilsagnTilAltinnMapper.tilAltinnKorrespondanse(
                    tilsagn = tilsagn,
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
            }
    }
}

fun tilsagnJournalpost(
    pdf: ByteArray,
    tilsagnId: UUID,
    deltaker: NorskIdent,
    arrangorOrganisasjonsnummer: Organisasjonsnummer,
    arrangorNavn: String,
    fagsakId: String,
): Journalpost = Journalpost(
    tittel = "Tilsagnsbrev",
    journalposttype = "UTGAAENDE",
    avsenderMottaker = Journalpost.AvsenderMottaker(
        id = arrangorOrganisasjonsnummer.value,
        idType = "ORGNR",
        navn = arrangorNavn,
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
