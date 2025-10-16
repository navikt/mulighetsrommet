package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.ktor.server.config.ApplicationConfigurationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
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

class InitialLoadGjennomforingerV2(
    private val config: Config,
    private val db: ApiDatabase,
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
) {
    data class Config(
        val topic: String?,
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    enum class GjennomforingType {
        ENKELTPLASS,
        GRUPPE,
    }

    @Serializable
    data class Input(
        val type: GjennomforingType,
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

            logger.info("v2: Relaster gjennomføringer på topic input=$input")

            if (input.ids != null) {
                when (input.type) {
                    GjennomforingType.GRUPPE -> initialLoadTiltaksgjennomforingerGruppeByIds(input.ids)
                    GjennomforingType.ENKELTPLASS -> initialLoadTiltaksgjennomforingerEnkeltplassByIds(input.ids)
                }
            }

            if (input.tiltakskoder != null) {
                when (input.type) {
                    GjennomforingType.GRUPPE ->
                        initialLoadTiltaksgjennomforingerGruppe(
                            tiltakskoder = input.tiltakskoder,
                        )

                    GjennomforingType.ENKELTPLASS -> initialLoadTiltaksgjennomforingerEnkeltplass(tiltakskoder = input.tiltakskoder)
                }
            }

            if (input.avtaleId != null) {
                when (input.type) {
                    GjennomforingType.GRUPPE ->
                        initialLoadTiltaksgjennomforingerGruppeByAvtale(input.avtaleId)

                    GjennomforingType.ENKELTPLASS -> Unit
                }
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

    private suspend fun initialLoadTiltaksgjennomforingerGruppe(
        tiltakskoder: List<Tiltakskode>,
    ): Unit = db.session {
        val tiltakstypeIder = tiltakskoder.map { queries.tiltakstype.getByTiltakskode(it).id }

        val total = paginateFanOut(
            { pagination: Pagination ->
                logger.info("v2: Henter gjennomføringer pagination=$pagination")
                val result = queries.gjennomforing.getAll(
                    pagination = pagination,
                    tiltakstypeIder = tiltakstypeIder,
                )
                result.items
            },
        ) {
            publishGruppe(it)
        }

        logger.info("v2: Antall relastet på topic: $total")
    }

    private suspend fun initialLoadTiltaksgjennomforingerEnkeltplass(
        tiltakskoder: List<Tiltakskode>,
    ): Unit = db.session {
        val tiltakstypeIder = tiltakskoder.map { queries.tiltakstype.getByTiltakskode(it).id }

        val total = queries.enkeltplass.getAll(
            tiltakstypeIder = tiltakstypeIder,
        )
        total.map { publishEnkeltplass(it) }

        logger.info("v2: Antall relastet på topic: $total")
    }

    private fun initialLoadTiltaksgjennomforingerGruppeByIds(ids: List<UUID>) = db.session {
        ids.forEach { id ->
            val gjennomforing = queries.gjennomforing.get(id)
            if (gjennomforing == null) {
                logger.info("v2: Sender tombstone for id $id")
                retractGruppe(id)
            } else {
                logger.info("v2: Publiserer melding for $id")
                publishGruppe(gjennomforing)
            }
        }
    }

    private fun initialLoadTiltaksgjennomforingerEnkeltplassByIds(ids: List<UUID>) = db.session {
        ids.forEach { id ->
            val enkeltplass = queries.enkeltplass.get(id)
            if (enkeltplass == null) {
                logger.info("v2: Sender tombstone for enkeltplass id $id")
                retractEnkeltplass(id)
            } else {
                logger.info("v2: Publiserer melding for enkeltplass id $id")
                publishEnkeltplass(enkeltplass)
            }
        }
    }

    private fun initialLoadTiltaksgjennomforingerGruppeByAvtale(avtaleId: UUID) = db.session {
        queries.gjennomforing.getAll(avtaleId = avtaleId).items.forEach {
            publishGruppe(it)
        }
    }

    private fun publishGruppe(dto: Gjennomforing) {
        val message = TiltaksgjennomforingV2Mapper.fromGruppe(dto)

        if (config.topic == null) {
            throw ApplicationConfigurationException("Mangler InitialLoadGjennomforingerV2 topic - publish forsøkt")
        }
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            message.id.toString().toByteArray(),
            Json.encodeToString(message).toByteArray(),
        )

        kafkaProducerClient.sendSync(record)
    }

    fun retractGruppe(id: UUID) {
        if (config.topic == null) {
            throw ApplicationConfigurationException("Mangler InitialLoadGjennomforingerV2 topic - retract forsøkt")
        }
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            id.toString().toByteArray(),
            null,
        )
        kafkaProducerClient.sendSync(record)
    }

    private fun publishEnkeltplass(dto: Enkeltplass) {
        val message = TiltaksgjennomforingV2Mapper.fromEnkeltplass(dto)

        if (config.topic == null) {
            throw ApplicationConfigurationException("Mangler InitialLoadGjennomforingerV2 topic - publish forsøkt")
        }
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            message.id.toString().toByteArray(),
            Json.encodeToString(message).toByteArray(),
        )

        kafkaProducerClient.sendSync(record)
    }

    fun retractEnkeltplass(id: UUID) {
        if (config.topic == null) {
            throw ApplicationConfigurationException("Mangler InitialLoadGjennomforingerV2 topic - retract forsøkt")
        }
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            id.toString().toByteArray(),
            null,
        )
        kafkaProducerClient.sendSync(record)
    }
}
