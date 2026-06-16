package no.nav.mulighetsrommet.api.tilskuddbehandling.task

import arrow.core.Either
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.FailureHandler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistError
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistRequest
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistResponse
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Duration.ofMinutes
import java.time.Instant
import java.util.UUID

class DistribuerVedtaksbrev(
    private val db: ApiDatabase,
    private val dokdistClient: DokdistClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val vedtakId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .onFailure(FailureHandler.ExponentialBackoffFailureHandler<TaskData>(ofMinutes(5)))
        .executeSuspend { inst, _ ->
            distribuerDok(inst.data.vedtakId).onLeft { message ->
                throw Exception("Feil distribuering av vedtaksbrev for vedtak id=${inst.data.vedtakId}: $message")
            }
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(tilskuddBehandlingId: UUID, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(tilskuddBehandlingId))
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun distribuerDok(tilskuddBehandlingId: UUID): Either<DokdistError, DokdistResponse> = db.transaction {
        logger.info("Distribuerer journalpost for vedtak id: $tilskuddBehandlingId")

        val tilskudd = queries.tilskuddBehandling.getOrError(tilskuddBehandlingId)
        require(tilskudd.vedtakJournalpostId != null) { "Vedtak med id=$tilskuddBehandlingId har ingen journalpostId, distribuering ikke mulig" }

        dokdistClient.distribuerJournalpost(
            journalpostId = tilskudd.vedtakJournalpostId,
            accessType = AccessType.M2M,
            distribusjonstype = DokdistRequest.DistribusjonsType.ANNET,
            adresse = null,
        ).map { response ->
            queries.tilskuddBehandling.setJournalpostDistribueringId(tilskuddBehandlingId, response.bestillingsId)
            response
        }
    }
}
