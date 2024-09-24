package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.utdanning.Utdanning
import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.jvm.optionals.getOrNull

class SynchronizeUtdanninger(
    private val db: Database,
    private val utdanningClient: UtdanningClient,
    private val config: Config,
    private val slack: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String? = null,
    ) {
        fun toSchedule(): Schedule {
            return if (disabled) {
                DisabledSchedule()
            } else {
                Schedules.cron(cronPattern)
            }
        }
    }

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.name, config.toSchedule())
        .onFailure { failure, _ ->
            val cause = failure.cause.getOrNull()?.message
            val stackTrace = failure.cause.getOrNull()?.stackTraceToString()
            slack.sendMessage(
                """
                Klarte ikke synkronisere utdanninger fra utdanning.no.
                Konsekvensen er at databasen over utdanninger i løsningen kan være utdatert.
                Detaljer: $cause
                Stacktrace: $stackTrace
                """.trimIndent(),
            )
        }
        .execute { instance, _ ->
            MDC.put("correlationId", instance.id)

            logger.info("Synkroniserer utdanninger fra utdanning.no...")

            runBlocking {
                syncUtdanninger()
            }
        }

    suspend fun syncUtdanninger() {
        utdanningClient
            .getUtdanninger()
            .filter { it.erYrkesfagligOgAktiv() }
            .forEach { saveUtdanning(it) }
    }

    private fun saveUtdanning(utdanning: Utdanning) {
        @Language("PostgreSQL")
        val query = """
            insert into utdanning (id, navn, utdanningsprogram, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start)
            values (:id, :navn, :utdanningsprogram::utdanning_program, :sluttkompetanse::utdanning_sluttkompetanse, :aktiv, :utdanningstatus::utdanning_status, :utdanningslop, :programlop_start )
            on conflict (id) do update set
                navn = excluded.navn,
                utdanningsprogram = excluded.utdanningsprogram,
                sluttkompetanse = excluded.sluttkompetanse,
                aktiv = excluded.aktiv,
                utdanningstatus = excluded.utdanningstatus,
                utdanningslop = excluded.utdanningslop,
                programlop_start = excluded.programlop_start
        """.trimIndent()

        @Language("PostgreSQL")
        val nuskodeInnholdInsertQuery = """
            insert into utdanning_nus_kode_innhold(title, nus_kode, aktiv)
            values(:title, :nus_kode, true)
            on conflict (nus_kode) do update set
                title = excluded.title
        """.trimIndent()

        @Language("PostgreSQL")
        val nusKodeKoblingforUtdanningQuery = """
            insert into utdanning_nus_kode(utdanning_id, nus_kode_id)
            values (:utdanning_id, :nus_kode_id)
        """.trimIndent()

        db.transaction { tx ->
            queryOf(
                query,
                mapOf(
                    "id" to utdanning.id,
                    "navn" to utdanning.navn,
                    "utdanningsprogram" to utdanning.utdanningsprogram?.name,
                    "sluttkompetanse" to utdanning.sluttkompetanse?.name,
                    "aktiv" to utdanning.aktiv,
                    "utdanningstatus" to utdanning.utdanningstatus.name,
                    "utdanningslop" to db.createTextArray(utdanning.utdanningslop),
                    "programlop_start" to utdanning.utdanningslop.first(),
                ),
            ).asExecute.let { tx.run(it) }

            utdanning.nus.forEach { nus ->
                queryOf(
                    nuskodeInnholdInsertQuery,
                    mapOf("title" to nus.nus_navn_nb, "nus_kode" to nus.nus_kode),
                ).asExecute.runWithSession(tx)

                queryOf(
                    nusKodeKoblingforUtdanningQuery,
                    mapOf("utdanning_id" to utdanning.id, "nus_kode_id" to nus.nus_kode),
                ).asExecute.let { tx.run(it) }
            }
        }
    }
}
