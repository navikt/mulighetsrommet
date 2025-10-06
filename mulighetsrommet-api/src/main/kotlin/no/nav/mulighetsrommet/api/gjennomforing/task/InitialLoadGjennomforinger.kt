package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class InitialLoadGjennomforinger(
    private val config: Config,
    private val db: ApiDatabase,
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
) {
    data class Config(
        val topic: String,
    )

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
            publish(it)
        }

        logger.info("Antall relastet på topic: $total")
    }

    private fun initialLoadTiltaksgjennomforingerByIds(ids: List<UUID>) = db.session {
        ids.forEach { id ->
            val gjennomforing = queries.gjennomforing.get(id)
            if (gjennomforing == null) {
                logger.info("Sender tombstone for id $id")
                retract(id)
            } else {
                logger.info("Publiserer melding for $id")
                publish(gjennomforing)
            }
        }
    }

    private fun initialLoadTiltaksgjennomforingerByAvtale(avtaleId: UUID) = db.session {
        queries.gjennomforing.getAll(avtaleId = avtaleId).items.forEach {
            publish(it)
        }
    }

    private fun publish(dto: Gjennomforing) {
        val message = TiltaksgjennomforingV1Mapper.fromGjennomforing(dto)

        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            message.id.toString().toByteArray(),
            Json.encodeToString(message).toByteArray(),
        )

        kafkaProducerClient.sendSync(record)
    }

    fun retract(id: UUID) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            id.toString().toByteArray(),
            null,
        )
        kafkaProducerClient.sendSync(record)
    }
}
