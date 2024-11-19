package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class InitialLoadTiltaksgjennomforinger(
    database: Database,
    private val tiltakstyper: TiltakstypeRepository,
    private val gjennomforinger: TiltaksgjennomforingRepository,
    private val gjennomforingProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskInput(
        val ids: List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            >? = null,
        val opphav: ArenaMigrering.Opphav? = null,
        val tiltakskoder: List<Tiltakskode>? = null,
    )

    val task: OneTimeTask<TaskInput> = Tasks
        .oneTime(javaClass.simpleName, TaskInput::class.java)
        .execute { instance, _ ->
            val input = instance.data

            logger.info("Relaster gjennomføringer på topic input=$input")

            if (input.ids != null) {
                initialLoadTiltaksgjennomforingerByIds(input.ids)
            } else if (input.tiltakskoder != null) {
                runBlocking {
                    initialLoadTiltaksgjennomforinger(
                        tiltakskoder = input.tiltakskoder,
                        opphav = input.opphav,
                    )
                }
            }
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(input: TaskInput, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), input)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltaksgjennomforinger(
        tiltakskoder: List<Tiltakskode>,
        opphav: ArenaMigrering.Opphav?,
    ) {
        val tiltakstypeIder = tiltakskoder.map { tiltakstyper.getByTiltakskode(it).id }

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter gjennomføringer pagination=$pagination")
                val result = gjennomforinger.getAll(
                    pagination = pagination,
                    opphav = opphav,
                    tiltakstypeIder = tiltakstypeIder,
                )
                result.items
            },
        ) {
            gjennomforingProducer.publish(it.toTiltaksgjennomforingV1Dto())
        }

        logger.info("Antall relastet på topic: $total")
    }

    private fun initialLoadTiltaksgjennomforingerByIds(ids: List<UUID>) {
        ids.forEach { id ->
            val gjennomforing = gjennomforinger.get(id)
            if (gjennomforing == null) {
                logger.info("Sender tombstone for id $id")
                gjennomforingProducer.retract(id)
            } else {
                logger.info("Publiserer melding for $id")
                gjennomforingProducer.publish(gjennomforing.toTiltaksgjennomforingV1Dto())
            }
        }
    }
}
