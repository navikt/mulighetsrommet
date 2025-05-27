package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class InitialLoadGjennomforinger(
    private val db: ApiDatabase,
    private val gjennomforingProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class Input(
        val ids: List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            >? = null,
        val tiltakskoder: List<Tiltakskode>? = null,
        @Serializable(with = UUIDSerializer::class)
        val avtaleId: UUID? = null,
    )

    val task: OneTimeTask<Input> = Tasks
        .oneTime(javaClass.simpleName, Input::class.java)
        .executeSuspend { instance, _ ->
            val input = instance.data

            logger.info("Relaster gjennomføringer på topic input=$input")

            if (input.ids != null) {
                initialLoadTiltaksgjennomforingerByIds(input.ids)
            }

            if (input.tiltakskoder != null) {
                initialLoadTiltaksgjennomforinger(
                    tiltakskoder = input.tiltakskoder,
                )
            }

            if (input.avtaleId != null) {
                initialLoadTiltaksgjennomforingerByAvtale(input.avtaleId)
            }
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(input: Input, id: UUID = UUID.randomUUID(), startTime: Instant = Instant.now()): UUID {
        val instance = task.instance(id.toString(), input)
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltaksgjennomforinger(
        tiltakskoder: List<Tiltakskode>,
    ): Unit = db.session {
        val tiltakstypeIder = tiltakskoder.map { queries.tiltakstype.getByTiltakskode(it).id }

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter gjennomføringer pagination=$pagination")
                val result = queries.gjennomforing.getAll(
                    pagination = pagination,
                    tiltakstypeIder = tiltakstypeIder,
                )
                result.items
            },
        ) {
            gjennomforingProducer.publish(it.toTiltaksgjennomforingV1Dto())
        }

        logger.info("Antall relastet på topic: $total")
    }

    private fun initialLoadTiltaksgjennomforingerByIds(ids: List<UUID>) = db.session {
        ids.forEach { id ->
            val gjennomforing = queries.gjennomforing.get(id)
            if (gjennomforing == null) {
                logger.info("Sender tombstone for id $id")
                gjennomforingProducer.retract(id)
            } else {
                logger.info("Publiserer melding for $id")
                gjennomforingProducer.publish(gjennomforing.toTiltaksgjennomforingV1Dto())
            }
        }
    }

    private fun initialLoadTiltaksgjennomforingerByAvtale(avtaleId: UUID) = db.session {
        queries.gjennomforing.getAll(avtaleId = avtaleId).items.forEach {
            gjennomforingProducer.publish(it.toTiltaksgjennomforingV1Dto())
        }
    }
}
