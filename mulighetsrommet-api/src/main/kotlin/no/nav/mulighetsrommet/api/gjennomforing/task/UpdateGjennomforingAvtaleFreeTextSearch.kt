package no.nav.mulighetsrommet.api.gjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingAvtaleService
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class UpdateGjennomforingAvtaleFreeTextSearch(
    private val db: ApiDatabase,
    private val avtaleGjennomforingService: GjennomforingAvtaleService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName, Void::class.java)
        .execute { _, _ ->
            execute()
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(id: UUID = UUID.randomUUID(), startTime: Instant = Instant.now()): UUID {
        val instance = task.instance(id.toString())
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    fun execute() {
        logger.info("Oppdaterer FTS for alle gjennomføringer av type AVTALE")

        val gjennomforinger = getGjennomforingerAvtaleIds()

        gjennomforinger.forEach { id ->
            logger.info("Oppdaterer FTS på gjennomføring id=$id")
            avtaleGjennomforingService.updateFreeTextSearch(id)
        }

        logger.info("Oppdaterte FTS for ${gjennomforinger.size} gjennomføringer")
    }

    private fun getGjennomforingerAvtaleIds(): List<UUID> = db.session {
        @Language("PostgreSQL")
        val query = """
            select id
            from gjennomforing
            where gjennomforing_type = 'AVTALE'
        """.trimIndent()

        session.list(queryOf(query)) { it.uuid("id") }
    }
}
