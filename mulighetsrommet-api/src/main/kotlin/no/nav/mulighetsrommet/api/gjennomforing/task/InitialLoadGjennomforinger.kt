package no.nav.mulighetsrommet.api.gjennomforing.task

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
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
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltakskoder
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

            input.tiltakskoder?.filter { Tiltakskoder.isGruppetiltak(it) }?.toNonEmptyListOrNull()?.let {
                initialLoadGruppetiltakByTiltakskode(tiltakskoder = it)
            }

            input.tiltakskoder?.filter { Tiltakskoder.isEnkeltplassTiltak(it) }?.toNonEmptyListOrNull()?.let {
                initialLoadEnkeltplassByTiltakskode(tiltakskoder = it)
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
        tiltakskoder: NonEmptyList<Tiltakskode>,
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

        logger.info("Antall gruppetiltak relastet på topic: $total")
    }

    private suspend fun initialLoadEnkeltplassByTiltakskode(
        tiltakskoder: NonEmptyList<Tiltakskode>,
    ): Unit = db.session {
        val tiltakstyper = tiltakskoder.map { queries.tiltakstype.getByTiltakskode(it).id }

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("Henter enkeltplasser pagination=$pagination")
                val result = queries.enkeltplass.getAll(
                    pagination = pagination,
                    tiltakstyper = tiltakstyper,
                )
                result.items
            },
        ) {
            publish(it)
        }

        logger.info("Antall enkeltplasser relastet på topic: $total")
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

    private fun initialLoadByAvtale(avtaleId: UUID) = db.session {
        queries.gjennomforing.getAll(avtaleId = avtaleId).items.forEach {
            publish(it)
        }
    }

    private fun publish(gjennomforing: Gjennomforing) {
        val gjennomforingV1 = TiltaksgjennomforingV1Mapper.fromGjennomforing(gjennomforing)
        val recordV1: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV1Topic,
            gjennomforingV1.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV1).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV1)

        val gjennomforingV2 = TiltaksgjennomforingV2Mapper.fromGruppe(gjennomforing)
        val recordV2: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV2).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV2)
    }

    private fun publish(enkeltplass: Enkeltplass) {
        val gjennomforingV2 = TiltaksgjennomforingV2Mapper.fromEnkeltplass(enkeltplass)
        val recordV2: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.gjennomforinvV2Topic,
            gjennomforingV2.id.toString().toByteArray(),
            Json.encodeToString(gjennomforingV2).toByteArray(),
        )
        kafkaProducerClient.sendSync(recordV2)
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
