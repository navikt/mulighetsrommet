package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.paginateFanOut
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.slf4j.MDC
import java.time.Instant
import java.util.*

class RetractEgenRegiTiltak(
    database: Database,
    private val tiltakstyper: TiltakstypeRepository,
    private val gjennomforinger: TiltaksgjennomforingRepository,
    private val gjennomforingProducer: TiltaksgjennomforingKafkaProducer,
) {

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.name)
        .execute { instance, _ ->
            MDC.put("correlationId", instance.id)

            runBlocking {
                retractEgenRegiTiltak()
            }
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString())
        client.schedule(instance, startTime)
        return id
    }

    private suspend fun retractEgenRegiTiltak(): Int {
        val tiltakstyper = tiltakstyper.getAll()
            .items
            .filter { Tiltakskoder.isEgenRegiTiltak(it.arenaKode) }
            .map { it.id }

        return paginateFanOut(
            { pagination: Pagination ->
                val result = gjennomforinger.getAll(
                    pagination = pagination,
                    tiltakstypeIder = tiltakstyper,
                )
                result.items
            },
        ) {
            gjennomforingProducer.retract(it.id)
        }
    }
}
