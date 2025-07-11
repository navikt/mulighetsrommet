package no.nav.mulighetsrommet.api.utbetaling.task

import arrow.core.Either
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.arrangorflate.ArrangorFlateService
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateBeregning
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrFlateUtbetalingStatus
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkError
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.dokark.Journalpost
import no.nav.mulighetsrommet.api.pdfgen.*
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.toReadableName
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
            journalfor(inst.data.utbetalingId, inst.data.vedlegg)
        }

    fun schedule(utbetalingId: UUID, startTime: Instant, tx: TransactionalSession, vedlegg: List<Vedlegg>): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(utbetalingId, vedlegg))
        val client = transactionalSchedulerClient(task, tx.connection.underlying)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalfor(id: UUID, vedlegg: List<Vedlegg>): Either<DokarkError, DokarkResponse> = db.session {
        logger.info("Journalfører utbetaling med id: $id")

        val utbetaling = requireNotNull(queries.utbetaling.get(id)) { "Fant ikke utbetaling med id=$id" }
        require(utbetaling.innsender != null) { "utbetaling må være godkjent" }

        val gjennomforing = queries.gjennomforing.get(utbetaling.gjennomforing.id)
        requireNotNull(gjennomforing) { "Fant ikke gjennomforing til utbetaling med id=$id" }

        val fagsakId = gjennomforing.tiltaksnummer ?: gjennomforing.lopenummer

        val pdf = run {
            val arrflateUtbetaling = arrangorFlateService.toArrFlateUtbetaling(utbetaling)
            pdf.utbetalingJournalpost(
                utbetaling = UtbetalingPdfDto(
                    status = ArrFlateUtbetalingStatus.toReadableName(arrflateUtbetaling.status),
                    periodeStart = arrflateUtbetaling.periode.start,
                    periodeSlutt = arrflateUtbetaling.periode.slutt.minusDays(1),
                    arrangor = ArrangorPdf(
                        organisasjonsnummer = arrflateUtbetaling.arrangor.organisasjonsnummer.value,
                        navn = arrflateUtbetaling.arrangor.navn,
                    ),
                    godkjentAvArrangorTidspunkt = arrflateUtbetaling.godkjentAvArrangorTidspunkt,
                    createdAt = arrflateUtbetaling.createdAt,
                    gjennomforing = GjennomforingPdf(
                        navn = arrflateUtbetaling.gjennomforing.navn,
                    ),
                    tiltakstype = TiltakstypePdf(
                        navn = arrflateUtbetaling.tiltakstype.navn,
                    ),
                    // TODO: kan mapping gjøres fra Utbetaling -> UtbetalingPdf i stedet for å mappe Utbetaling -> ArrflateUtbetaling -> UtbetalingPdf?
                    beregning = when (arrflateUtbetaling.beregning) {
                        is ArrFlateBeregning.Fri -> BeregningPdf(
                            belop = arrflateUtbetaling.beregning.belop,
                            antallManedsverk = null,
                            deltakelser = emptyList(),
                            stengt = emptyList(),
                        )

                        is ArrFlateBeregning.PrisPerManedsverkMedDeltakelsesmengder -> BeregningPdf(
                            belop = arrflateUtbetaling.beregning.belop,
                            antallManedsverk = arrflateUtbetaling.beregning.antallManedsverk,
                            deltakelser = arrflateUtbetaling.beregning.deltakelser.map {
                                DeltakerPdf(
                                    startDato = it.periodeStartDato,
                                    sluttDato = it.periodeSluttDato,
                                    perioder = it.perioderMedDeltakelsesmengde,
                                    manedsverk = it.faktor,
                                    person = it.person?.let { person ->
                                        PersonPdf(
                                            navn = person.navn,
                                            fodselsdato = person.fodselsdato,
                                            fodselsaar = person.fodselsaar,
                                        )
                                    },
                                )
                            },
                            stengt = arrflateUtbetaling.beregning.stengt.map {
                                StengtPeriodePdf(
                                    periode = it.periode,
                                    beskrivelse = it.beskrivelse,
                                )
                            },
                        )

                        is ArrFlateBeregning.PrisPerManedsverk -> BeregningPdf(
                            belop = arrflateUtbetaling.beregning.belop,
                            antallManedsverk = arrflateUtbetaling.beregning.antallManedsverk,
                            deltakelser = arrflateUtbetaling.beregning.deltakelser.map {
                                DeltakerPdf(
                                    startDato = it.periodeStartDato,
                                    sluttDato = it.periodeSluttDato,
                                    perioder = listOf(),
                                    manedsverk = it.faktor,
                                    person = it.person?.let { person ->
                                        PersonPdf(
                                            navn = person.navn,
                                            fodselsdato = person.fodselsdato,
                                            fodselsaar = person.fodselsaar,
                                        )
                                    },
                                )
                            },
                            stengt = arrflateUtbetaling.beregning.stengt.map {
                                StengtPeriodePdf(
                                    periode = it.periode,
                                    beskrivelse = it.beskrivelse,
                                )
                            },
                        )

                        is ArrFlateBeregning.PrisPerUkesverk -> BeregningPdf(
                            belop = arrflateUtbetaling.beregning.belop,
                            // TODO støtte ukesverk?
                            antallManedsverk = null,
                            // TODO deltakelser med eller uten deltakelsesprosent?
                            deltakelser = emptyList(),
                            stengt = arrflateUtbetaling.beregning.stengt.map {
                                StengtPeriodePdf(
                                    periode = it.periode,
                                    beskrivelse = it.beskrivelse,
                                )
                            },
                        )
                    },
                    betalingsinformasjon = arrflateUtbetaling.betalingsinformasjon,
                    linjer = arrflateUtbetaling.linjer.map {
                        UtbetalingslinjerPdfDto(
                            id = it.id,
                            tilsagn = it.tilsagn,
                            status = toReadableName(it.status),
                            belop = it.belop,
                            statusSistOppdatert = it.statusSistOppdatert,
                        )
                    },
                    type = UtbetalingType.from(utbetaling),
                ),
            )
        }

        val journalpost = utbetalingJournalpost(pdf, utbetaling.id, utbetaling.arrangor, fagsakId, vedlegg)

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
