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
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.*

class InitialLoadTiltaksgjennomforinger(
    database: Database,
    private val tiltakstyper: TiltakstypeRepository,
    private val gjennomforinger: TiltaksgjennomforingRepository,
    private val gjennomforingProducer: TiltaksgjennomforingKafkaProducer,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class Input(
        val opphav: ArenaMigrering.Opphav? = null,
        val tiltakstyper: List<Tiltakskode>? = null,
    )

    val task: OneTimeTask<Input> = Tasks
        .oneTime(javaClass.name, Input::class.java)
        .execute { instance, context ->
            val input = instance.data

            logger.info("Running task ${instance.taskName} with input=$input")

            MDC.put("correlationId", instance.id)

            runBlocking {
                val job = async {
                    initialLoadTiltaksgjennomforinger(input)
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

    fun schedule(input: Input, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), input)
        client.schedule(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltaksgjennomforinger(input: Input): Int {
        val tiltakstypeIder = (input.tiltakstyper ?: emptyList())
            .map { tiltakstyper.getByTiltakskode(it).id }

        return paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter gjennomføringer pagination=$pagination")
                val result = gjennomforinger.getAll(
                    pagination = pagination,
                    opphav = input.opphav,
                    tiltakstypeIder = tiltakstypeIder,
                )
                result.items
            },
        ) {
            gjennomforingProducer.publish(TiltaksgjennomforingDto.from(it))
        }
    }
}
