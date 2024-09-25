package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.utdanning.Programomrade
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
        val (programomrader, utdanninger) = utdanningClient
            .getUtdanninger()
            .partition { it.sluttkompetanse == null && it.utdanningId == null && it.utdanningslop.size == 1 }

        programomrader.map { it.toProgramomrade() }.forEach { saveProgramomrade(it) }
        utdanninger.forEach { saveUtdanning(it) }
    }

    private fun saveProgramomrade(programomrade: Programomrade) {
        @Language("PostgreSQL")
        val query = """
            insert into utdanning_programomrade (navn, programomradekode, utdanningsprogram)
            values (:navn, :programomradekode, :utdanningsprogram::utdanning_program)
            on conflict (programomradekode) do update set
                navn = excluded.navn,
                utdanningsprogram = excluded.utdanningsprogram
        """.trimIndent()

        db.transaction { tx ->
            queryOf(
                query,
                mapOf(
                    "navn" to programomrade.navn,
                    "programomradekode" to programomrade.programomradekode,
                    "utdanningsprogram" to programomrade.utdanningsprogram?.name,
                ),
            ).asExecute.let { tx.run(it) }
        }
    }

    private fun saveUtdanning(utdanning: Utdanning) {
        @Language("PostgreSQL")
        val getIdForProgramomradeQuery = """
            select id from utdanning_programomrade where programomradekode = :programomradekode
        """.trimIndent()

        val programomradeId = db.transaction { tx ->
            queryOf(
                getIdForProgramomradeQuery,
                mapOf("programomradekode" to utdanning.utdanningslop.first()),
            ).map { it.uuid("id") }
                .asSingle
                .runWithSession(tx)
        }

        @Language("PostgreSQL")
        val upsertUtdanning = """
            insert into utdanning (utdanning_id, programomradekode, navn, utdanningsprogram, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start)
            values (:utdanning_id, :programomradekode, :navn, :utdanningsprogram::utdanning_program, :sluttkompetanse::utdanning_sluttkompetanse, :aktiv, :utdanningstatus::utdanning_status, :utdanningslop, :programlop_start::uuid)
            on conflict (utdanning_id) do update set
                programomradekode = excluded.programomradekode,
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
            insert into utdanning_nus_kode_innhold(title, nus_kode)
            values(:title, :nus_kode)
            on conflict (nus_kode) do update set
                title = excluded.title
        """.trimIndent()

        @Language("PostgreSQL")
        val nusKodeKoblingforUtdanningQuery = """
            insert into utdanning_nus_kode(utdanning_id, nus_kode)
            values (:utdanning_id, :nus_kode_id)
        """.trimIndent()

        db.transaction { tx ->
            queryOf(
                upsertUtdanning,
                mapOf(
                    "utdanning_id" to utdanning.utdanningId,
                    "programomradekode" to utdanning.programomradekode,
                    "navn" to utdanning.navn,
                    "utdanningsprogram" to utdanning.utdanningsprogram?.name,
                    "sluttkompetanse" to utdanning.sluttkompetanse?.name,
                    "aktiv" to utdanning.aktiv,
                    "utdanningstatus" to utdanning.utdanningstatus.name,
                    "utdanningslop" to db.createTextArray(utdanning.utdanningslop),
                    "programlop_start" to programomradeId,
                ),
            ).asExecute.let { tx.run(it) }

            utdanning.nusKodeverk.forEach { nus ->
                queryOf(
                    nuskodeInnholdInsertQuery,
                    mapOf("title" to nus.navn, "nus_kode" to nus.kode),
                ).asExecute.runWithSession(tx)

                queryOf(
                    nusKodeKoblingforUtdanningQuery,
                    mapOf("utdanning_id" to utdanning.utdanningId, "nus_kode_id" to nus.kode),
                ).asExecute.let { tx.run(it) }
            }
        }
    }
}
