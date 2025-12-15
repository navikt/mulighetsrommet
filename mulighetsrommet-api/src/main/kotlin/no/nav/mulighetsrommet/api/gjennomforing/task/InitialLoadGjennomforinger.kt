package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV1Mapper
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltak
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltakskoder
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
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
) {
    data class Config(
        val gjennomforinvV1Topic: String,
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

            input.tiltakskode?.takeIf { Tiltakskoder.isGruppetiltak(it) }?.let {
                initialLoadGruppetiltakByTiltakskode(it)
            }

            input.tiltakskode?.takeIf { Tiltakskoder.isEnkeltplassTiltak(it) }?.let {
                initialLoadEnkeltplassByTiltakskode(it)
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

    private suspend fun initialLoadGruppetiltakByTiltakskode(
        tiltakskode: Tiltakskode,
    ): Unit = db.session {
        val tiltakstypeId = queries.tiltakstype.getByTiltakskode(tiltakskode).id

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter gjennomføringer pagination=$pagination")
                val result = queries.gjennomforing.getAllGruppetiltak(
                    pagination = pagination,
                    tiltakstypeIder = listOf(tiltakstypeId),
                )
                result.items
            },
        ) {
            publish(queries.gjennomforing.getGruppetiltakOrError(it.id))
        }

        logger.info("Antall gruppetiltak relastet på topic: $total")
    }

    private suspend fun initialLoadEnkeltplassByTiltakskode(
        tiltakskode: Tiltakskode,
    ): Unit = db.session {
        val tiltakstypeId = queries.tiltakstype.getByTiltakskode(tiltakskode).id

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter enkeltplasser pagination=$pagination")
                val result = queries.gjennomforing.getAllEnkeltplass(
                    pagination = pagination,
                    tiltakstyper = listOf(tiltakstypeId),
                )
                result.items
            },
        ) {
            publish(it)
        }

        logger.info("Antall enkeltplasser relastet på topic: $total")
    }

    private fun initialLoadGjennomforingerById(ids: List<UUID>) = db.session {
        ids.forEach { id ->
            val gruppetiltak = queries.gjennomforing.getGruppetiltak(id)
            if (gruppetiltak != null) {
                publish(gruppetiltak)
                return@forEach
            }

            val enkeltplass = queries.gjennomforing.getEnkeltplass(id)
            if (enkeltplass != null) {
                publish(enkeltplass)
                return@forEach
            }

            logger.warn("Fant ingen gjennomføring med id=$id")
        }
    }

    private fun initialLoadByAvtale(avtaleId: UUID) = db.session {
        queries.gjennomforing.getByAvtale(avtaleId).forEach {
            publish(it)
        }
    }

    private fun publish(gjennomforing: GjennomforingGruppetiltak) {
        val gjennomforingV1 = TiltaksgjennomforingV1Mapper.fromGjennomforing(gjennomforing)
        val recordV1: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV1Topic,
            gjennomforingV1.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV1).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV1)

        val gjennomforingV2: TiltaksgjennomforingV2Dto = TiltaksgjennomforingV2Mapper.fromGruppe(gjennomforing)
        val recordV2: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV2).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV2)
    }

    private fun publish(enkeltplass: Enkeltplass) {
        val gjennomforingV2: TiltaksgjennomforingV2Dto = TiltaksgjennomforingV2Mapper.fromEnkeltplass(enkeltplass)
        val recordV2: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV2).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV2)
    }
}
