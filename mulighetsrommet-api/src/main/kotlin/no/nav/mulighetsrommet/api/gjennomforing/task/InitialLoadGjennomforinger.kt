package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingDetaljerService
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class InitialLoadGjennomforinger(
    private val config: Config,
    private val db: ApiDatabase,
    private val gjennomforinger: GjennomforingDetaljerService,
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
) {
    data class Config(
        val gjennomforinvV2Topic: String,
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class Input(
        val ids: List<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            >? = null,
        val tiltakskode: Tiltakskode? = null,
        @Serializable(with = UUIDSerializer::class)
        val avtaleId: UUID? = null,
    )

    val task: OneTimeTask<Input> = Tasks
        .oneTime(javaClass.simpleName, Input::class.java)
        .executeSuspend { instance, _ ->
            val input = instance.data

            logger.info("Relaster gjennomføringer på topic input=$input")

            if (input.ids != null) {
                initialLoadGjennomforingerById(input.ids)
            }

            if (input.tiltakskode != null) {
                initialLoadByTiltakskode(input.tiltakskode)
            }

            if (input.avtaleId != null) {
                initialLoadByAvtale(input.avtaleId)
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

    suspend fun initialLoadByTiltakskode(
        tiltakskode: Tiltakskode,
    ): Unit = db.session {
        val tiltakstypeId = queries.tiltakstype.getByTiltakskode(tiltakskode).id

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter gjennomføringer, tiltakskode=$tiltakskode, pagination=$pagination")
                val result = queries.gjennomforing.getAll(
                    pagination = pagination,
                    tiltakstypeIder = listOf(tiltakstypeId),
                )
                result.items
            },
        ) {
            publish(it.id)
        }

        logger.info("Gjennomføringer relastet på topic, tiltakskode=$tiltakskode, antall=$total")
    }

    fun initialLoadGjennomforingerById(ids: List<UUID>) = db.session {
        ids.forEach { publish(it) }
    }

    private fun initialLoadByAvtale(avtaleId: UUID) = db.session {
        queries.gjennomforing.getByAvtale(avtaleId).forEach {
            publish(it.id)
        }
    }

    private fun publish(id: UUID) {
        val dto = gjennomforinger.getTiltaksgjennomforingV2Dto(id) ?: return run {
            logger.warn("Ingen gjennomføring med id=$id")
        }
        publish(dto)
    }

    private fun publish(gjennomforingV2: TiltaksgjennomforingV2Dto) {
        val recordV2: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV2).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV2)
    }
}
