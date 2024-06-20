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
            .collect { saveUtdanning(it) }
    }

    private fun saveUtdanning(utdanning: Utdanning) {
        val id = utdanning.sammenligning_id.substringAfter("u_")
        val interesser = utdanning.interesse.map { it.title }
        val sokeord = utdanning.sokeord.map { it.title }

        @Language("PostgreSQL")
        val query = """
            insert into utdanning (id, utdanning_no_sammenligning_id, title, description, aktiv, utdanningstype, sokeord, interesser)
            values (:id, :utdanning_no_sammenligning_id, :title, :description, true, :utdanningstype::utdanningstype[], :sokeord, :interesser)
            on conflict (id) do update set
                utdanning_no_sammenligning_id = excluded.utdanning_no_sammenligning_id,
                title = excluded.title,
                description = excluded.description,
                studieretning = excluded.studieretning,
                utdanningstype = excluded.utdanningstype::utdanningstype[],
                sokeord = excluded.sokeord,
                interesser = excluded.interesser
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
                    "id" to id,
                    "utdanning_no_sammenligning_id" to utdanning.sammenligning_id,
                    "title" to utdanning.title,
                    "description" to utdanning.body.summary,
                    "utdanningstype" to tx.createArrayOf(
                        "utdanningstype",
                        utdanning.utdtype.map { getUtdanningstype(it.utdt_kode) },
                    ),
                    "sokeord" to db.createTextArray(sokeord),
                    "interesser" to db.createTextArray(interesser),
                ),
            ).asExecute.let { tx.run(it) }

            utdanning.nus.forEach { nus ->
                queryOf(
                    nuskodeInnholdInsertQuery,
                    mapOf("title" to nus.title, "nus_kode" to nus.nus_kode),
                ).asExecute.runWithSession(tx)

                queryOf(
                    nusKodeKoblingforUtdanningQuery,
                    mapOf("utdanning_id" to id, "nus_kode_id" to nus.nus_kode),
                ).asExecute.let { tx.run(it) }
            }
        }
    }

    private fun getUtdanningstype(utdtKode: String): Utdanningstype {
        return when (utdtKode) {
            "VS" -> Utdanningstype.VIDEREGAENDE
            "UH" -> Utdanningstype.UNIVERSITET_OG_HOGSKOLE
            "TO" -> Utdanningstype.TILSKUDDSORDNING
            "FAG" -> Utdanningstype.FAGSKOLE
            else -> throw IllegalArgumentException("Ukjent utdanningstype")
        }
    }
}

enum class Utdanningstype {
    FAGSKOLE,
    TILSKUDDSORDNING,
    VIDEREGAENDE,
    UNIVERSITET_OG_HOGSKOLE,
}
