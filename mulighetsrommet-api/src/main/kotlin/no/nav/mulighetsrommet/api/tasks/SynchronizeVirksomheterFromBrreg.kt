package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.services.VirksomhetService
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Instant
import java.util.*

class SynchronizeVirksomheterFromBrreg(
    private val virksomhetService: VirksomhetService,
    private val database: Database,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.name)
        .execute { instance, _ ->
            logger.info("Running task ${instance.taskName}")

            MDC.put("correlationId", instance.id)

            runBlocking {
                synchronizeVirksomheterFromBrreg()
            }
        }

    private val client = SchedulerClient.Builder
        .create(database.getDatasource(), task)
        .build()

    fun schedule(startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        client.schedule(task.instance(id.toString()), startTime)
        return id
    }

    private suspend fun synchronizeVirksomheterFromBrreg() {
        @Language("PostgreSQL")
        val query = """
            with avtale_leverandor as (select distinct leverandor_organisasjonsnummer as orgnr from avtale),
                 avtale_arrangor as (select distinct organisasjonsnummer as orgnr from avtale_underleverandor),
                 gjennomforing_arrangor as (select distinct arrangor_organisasjonsnummer as orgnr from tiltaksgjennomforing),
                 alle_orgnr as (select orgnr
                                from avtale_leverandor
                                union
                                select orgnr
                                from avtale_arrangor
                                union
                                select orgnr
                                from gjennomforing_arrangor)
            select distinct orgnr
            from alle_orgnr;
        """.trimIndent()
        val orgnrs = queryOf(query)
            .map { it.string("orgnr") }
            .asList
            .let { database.run(it) }

        orgnrs.forEach { orgnr ->
            virksomhetService.syncVirksomhetFraBrreg(orgnr).onLeft {
                logger.warn("Klarte ikke synkronisere virksomhet med orgnr=$orgnr fra brreg: $it")
            }
        }
    }
}
