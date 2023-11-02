package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.*

class InitialLoadTiltaksgjennomforinger(
    database: Database,
    private val gjennomforinger: TiltaksgjennomforingRepository,
    private val gjennomforingProducer: TiltaksgjennomforingKafkaProducer,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks.oneTime(javaClass.name)
        .execute { instance, context ->
            logger.info("Running task ${instance.taskName}")

            MDC.put("correlationId", instance.id)

            runBlocking {
                val job = async {
                    initialLoadTiltaksgjennomforinger()
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

    private val client = SchedulerClient.Builder.create(database.getDatasource(), task).build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        client.schedule(task.instance(id.toString()), startTime)
        return id
    }

    private suspend fun initialLoadTiltaksgjennomforinger() = paginateFanOut({ pagination: PaginationParams ->
        logger.info("Henter gjennomføringer limit=${pagination.limit} offset=${pagination.offset}")
        gjennomforinger.getAll(pagination).second
    }) {
        gjennomforingProducer.publish(TiltaksgjennomforingDto.from(it))
    }
}
