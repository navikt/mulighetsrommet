package no.nav.mulighetsrommet.api.tiltakstype.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.tiltakstype.kafka.SisteTiltakstyperV2KafkaProducer
import no.nav.mulighetsrommet.database.Database
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class InitialLoadTiltakstyper(
    database: Database,
    private val tiltakstyper: TiltakstypeRepository,
    private val tiltakstypeProducer: SisteTiltakstyperV2KafkaProducer,
    private val sanityService: SanityService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.name)
        .execute { _, _ ->
            runBlocking {
                initialLoadTiltakstyper()
            }
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), task)
        .build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString())
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    private suspend fun initialLoadTiltakstyper() {
        tiltakstyper.getAll()
            .items
            .forEach { tiltakstype ->
                val tiltakskode = tiltakstype.tiltakskode
                if (tiltakskode != null) {
                    val eksternDto = requireNotNull(tiltakstyper.getEksternTiltakstype(tiltakstype.id)) {
                        "Klarte ikke hente ekstern tiltakstype for tiltakskode $tiltakskode"
                    }

                    logger.info("Publiserer tiltakstype til kafka id=${tiltakstype.id}")
                    tiltakstypeProducer.publish(eksternDto)
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
}
