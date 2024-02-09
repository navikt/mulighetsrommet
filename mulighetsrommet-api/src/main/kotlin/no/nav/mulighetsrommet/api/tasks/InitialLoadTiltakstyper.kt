package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.kafka.producers.TiltakstypeKafkaProducer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.*

class InitialLoadTiltakstyper(
    database: Database,
    private val tiltakstyper: TiltakstypeRepository,
    private val tiltakstypeProducer: TiltakstypeKafkaProducer,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<InitialLoadTiltaksgjennomforingerInput> = Tasks
        .oneTime(javaClass.name, InitialLoadTiltaksgjennomforingerInput::class.java)
        .execute { instance, context ->

            logger.info("Running task ${instance.taskName}")

            MDC.put("correlationId", instance.id)

            runBlocking {
                val job = async {
                    initialLoadTiltakstyper()
                }

                while (job.isActive) {
                    if (context.schedulerState.isShuttingDown) {
                        logger.info("Stopping task ${instance.taskName} due to shutdown signal")

                        job.cancelAndJoin()

                        logger.info("Task ${instance.taskName} stopped")
                    } else {
                        delay(1000)
                    }
                }
            }
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), task)
        .build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString())
        client.schedule(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltakstyper(): Int {
        return paginateFanOut(
            { pagination: PaginationParams ->
                logger.info("Henter tiltakstyper limit=${pagination.limit} offset=${pagination.offset}")
                tiltakstyper.getAllMedDeltakerregistreringsinnhold()
            },
        ) {
            tiltakstypeProducer.publish(it)
        }
    }
}
