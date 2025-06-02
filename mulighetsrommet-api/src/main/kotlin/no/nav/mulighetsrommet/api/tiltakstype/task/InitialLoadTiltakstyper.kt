package no.nav.mulighetsrommet.api.tiltakstype.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.model.TiltakstypeEksternV2Dto
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class InitialLoadTiltakstyper(
    private val config: Config,
    private val db: ApiDatabase,
    private val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    private val sanityService: SanityService,
) {
    data class Config(
        val topic: String,
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName)
        .executeSuspend { _, _ ->
            initialLoadTiltakstyper()
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString())
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltakstyper() = db.transaction {
        queries.tiltakstype.getAll().forEach { tiltakstype ->
            val tiltakskode = tiltakstype.tiltakskode
            if (tiltakskode != null) {
                val eksternDto = requireNotNull(queries.tiltakstype.getEksternTiltakstype(tiltakstype.id)) {
                    "Klarte ikke hente ekstern tiltakstype for tiltakskode $tiltakskode"
                }

                logger.info("Publiserer tiltakstype til kafka id=${tiltakstype.id}")
                publishToKafka(eksternDto)
            }

            if (tiltakstype.sanityId != null) {
                logger.info("Oppdaterer tiltakstype i Sanity id=${tiltakstype.id}")
                sanityService.patchSanityTiltakstype(
                    tiltakstype.sanityId,
                    tiltakstype.navn,
                    tiltakstype.innsatsgrupper,
                )
            }
        }
    }

    private fun publishToKafka(value: TiltakstypeEksternV2Dto) {
        val record: ProducerRecord<ByteArray, ByteArray?> = ProducerRecord(
            config.topic,
            value.id.toString().toByteArray(),
            Json.encodeToString(value).toByteArray(),
        )
        kafkaProducerClient.sendSync(record)
    }
}
