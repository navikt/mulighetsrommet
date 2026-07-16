package no.nav.mulighetsrommet.api.tilsagn.task

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
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Duration.ofMinutes
import java.time.Instant
import java.util.UUID

class DistribuerTilsagnsbrev(
    private val db: ApiDatabase,
    private val dokdistClient: DokdistClient,
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
            distribuerDok(inst.data.tilsagnId).onLeft { message ->
                throw Exception("Feil distribuering av tilsagnsbrev for tilsagn id=${inst.data.tilsagnId}: $message")
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

    suspend fun distribuerDok(tilsagnId: UUID): Either<DokdistError, DokdistResponse> = db.transaction {
        logger.info("Distribuerer journalpost for tilsagn id: $tilsagnId")

        val tilsagn = queries.tilsagn.getOrError(tilsagnId)
        require(tilsagn.journalpost?.id != null) { "Tilsagn med id=$tilsagnId har ingen journalpostId, distribuering ikke mulig" }

        val adresse = utledArrangorAdresse(tilsagn.arrangor.id)

        dokdistClient.distribuerJournalpost(
            journalpostId = tilsagn.journalpost.id,
            accessType = AccessType.M2M,
            distribusjonstype = DokdistRequest.DistribusjonsType.ANNET,
            adresse = adresse,
        ).map { response ->
            queries.tilsagn.setJournalpostDistribueringId(tilsagnId, response.bestillingsId)
            response
        }
    }

    fun utledArrangorAdresse(arrangorId: UUID): DokdistRequest.Adresse? {
        val arrangor = db.session { repository.arrangor.get(arrangorId) }

        return when (arrangor) {
            // Dokdist utleder adressen selv basert på organisasjonsnummer koblet til journalposten
            is Arrangor.Norsk -> null

            is Arrangor.Utenlandsk -> {
                val adresse = requireNotNull(arrangor.adresse) {
                    "Utenlandsk arrangør med id=${arrangor.id} mangler informasjon om adresse"
                }
                DokdistRequest.Adresse.UtenlandskPostadresse(
                    land = adresse.landKode,
                    adresselinje1 = adresse.gateNavn,
                    adresselinje2 = adresse.by,
                    adresselinje3 = null,
                )
            }
        }
    }
}
