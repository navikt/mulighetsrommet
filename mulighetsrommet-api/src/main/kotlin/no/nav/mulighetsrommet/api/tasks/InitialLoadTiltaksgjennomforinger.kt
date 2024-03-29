package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
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

    val task: OneTimeTask<InitialLoadTiltaksgjennomforingerInput> = Tasks
        .oneTime(javaClass.name, InitialLoadTiltaksgjennomforingerInput::class.java)
        .execute { instance, context ->
            val input = instance.data

            logger.info("Running task ${instance.taskName} with input=$input")

            MDC.put("correlationId", instance.id)

            runBlocking {
                val job = async {
                    initialLoadTiltaksgjennomforinger(opphav = input.opphav)
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
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(input: InitialLoadTiltaksgjennomforingerInput, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), input)
        client.schedule(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltaksgjennomforinger(opphav: ArenaMigrering.Opphav? = null): Int {
        return paginateFanOut(
            { pagination: PaginationParams ->
                logger.info("Henter gjennomføringer limit=${pagination.limit} offset=${pagination.offset}")
                val result = gjennomforinger.getAll(
                    pagination = pagination,
                    opphav = opphav,
                )
                result.second
            },
        ) {
            gjennomforingProducer.publish(TiltaksgjennomforingDto.from(it))
        }
    }
}

@Serializable
data class InitialLoadTiltaksgjennomforingerInput(
    val opphav: ArenaMigrering.Opphav?,
)
