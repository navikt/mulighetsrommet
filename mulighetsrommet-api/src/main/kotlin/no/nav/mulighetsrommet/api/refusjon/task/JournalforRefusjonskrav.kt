package no.nav.mulighetsrommet.api.refusjon.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResult
import no.nav.mulighetsrommet.api.pdfgen.Pdfgen
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.refusjon.refusjonskravJournalpost
import no.nav.mulighetsrommet.api.refusjon.toRefusjonskrav
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class JournalforRefusjonskrav(
    database: Database,
    private val refusjonskravRepository: RefusjonskravRepository,
    private val tilsagnService: TilsagnService,
    private val dokarkClient: DokarkClient,
    private val deltakerRepository: DeltakerRepository,
    private val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<UUID> = Tasks
        .oneTime(javaClass.simpleName, UUID::class.java)
        .executeSuspend { inst, _ ->
            journalforRefusjonskrav(inst.data)
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), task)
        .build()

    fun schedule(refusjonskravId: UUID, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), refusjonskravId)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun journalforRefusjonskrav(id: UUID) {
        logger.info("Journalfører refusjonskrav med id: $id")
        val krav = refusjonskravRepository.get(id)
        requireNotNull(krav) { "Fant ikke refusjonskrav med id=$id" }
        require(krav.status == RefusjonskravStatus.GODKJENT_AV_ARRANGOR) { "Krav må være godkjent" }

        val pdf = run {
            val tilsagn = tilsagnService.getArrangorflateTilsagnTilRefusjon(
                gjennomforingId = krav.gjennomforing.id,
                periode = krav.beregning.input.periode,
            )
            val refusjonsKravAft: RefusjonKravAft = toRefusjonskrav(pdl, deltakerRepository, krav)
            Pdfgen.refusjonJournalpost(refusjonsKravAft, tilsagn)
        }

        val result = dokarkClient.opprettJournalpost(
            refusjonskravJournalpost(pdf, krav.id, krav.arrangor.organisasjonsnummer),
            AccessType.M2M,
        )
        when (result) {
            is DokarkResult.Error -> throw Exception(
                "Feil ved opprettelse av journalpost. Message: ${result.message}",
            )
            is DokarkResult.Success -> {
                refusjonskravRepository.setJournalpostId(id, result.journalpostId)
            }
        }
    }
}
