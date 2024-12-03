package no.nav.mulighetsrommet.api.refusjon.task

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.pdfgen.Pdfgen
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.refusjon.refusjonskravJournalpost
import no.nav.mulighetsrommet.api.refusjon.toRefusjonskrav
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.util.*

class JournalforRefusjonskrav(
    private val refusjonskravRepository: RefusjonskravRepository,
    private val tilsagnService: TilsagnService,
    private val dokarkClient: DokarkClient,
    private val deltakerRepository: DeltakerRepository,
    private val pdl: HentAdressebeskyttetPersonBolkPdlQuery,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskInput(
        @Serializable(with = UUIDSerializer::class)
        val refusjonskravId: UUID,
    )

    val task: OneTimeTask<TaskInput> = Tasks
        .oneTime(javaClass.simpleName, TaskInput::class.java)
        .executeSuspend { inst, _ ->
            journalforRefusjonskrav(inst.data.refusjonskravId)
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

        dokarkClient.opprettJournalpost(
            refusjonskravJournalpost(pdf, krav.id, krav.arrangor.organisasjonsnummer),
            AccessType.M2M,
        )
            .onRight {
                refusjonskravRepository.setJournalpostId(id, it.journalpostId)
            }
            .onLeft {
                throw Exception("Feil ved opprettelse av journalpost. Message: ${it.message}")
            }
    }
}
