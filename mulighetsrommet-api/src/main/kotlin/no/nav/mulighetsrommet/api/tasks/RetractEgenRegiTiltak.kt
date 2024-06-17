package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.kafka.producers.TiltaksgjennomforingKafkaProducer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.intellij.lang.annotations.Language
import org.slf4j.MDC
import java.time.Instant
import java.util.*

class RetractEgenRegiTiltak(
    private val database: Database,
    private val gjennomforingProducer: TiltaksgjennomforingKafkaProducer,
) {

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.name)
        .execute { instance, _ ->
            MDC.put("correlationId", instance.id)

            @Language("PostgreSQL")
            val query = """
                select g.id
                from tiltaksgjennomforing g
                         join tiltakstype t on g.tiltakstype_id = t.id
                where t.arena_kode in ('INDJOBSTOT', 'IPSUNG', 'UTVAOONAV')
            """.trimIndent()

            val ids = database.useSession { session ->
                queryOf(query)
                    .map { it.uuid("id") }
                    .asList
                    .runWithSession(session)
            }

            ids.forEach {
                gjennomforingProducer.retract(it)
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
}
