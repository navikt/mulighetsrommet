package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient
import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningNoProgramomraade
import no.nav.mulighetsrommet.api.domain.dto.NusKodeverk
import no.nav.mulighetsrommet.api.domain.dto.Programomrade
import no.nav.mulighetsrommet.api.domain.dto.Utdanning
import no.nav.mulighetsrommet.api.domain.dto.Utdanningsprogram
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.jvm.optionals.getOrNull

class SynchronizeUtdanninger(
    private val db: Database,
    private val utdanningClient: UtdanningClient,
    config: Config,
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
        val allUtdanninger = utdanningClient.getUtdanninger()

        val (programomrader, utdanninger) = resolveRelevantUtdanninger(allUtdanninger)

        db.transaction { tx ->
            programomrader.forEach { saveProgramomrade(tx, it) }
            utdanninger.forEach { saveUtdanning(tx, it) }
        }
    }

    private fun saveProgramomrade(session: TransactionalSession, programomrade: Programomrade) {
        @Language("PostgreSQL")
        val query = """
            insert into utdanning_programomrade (navn, programomradekode, utdanningsprogram)
            values (:navn, :programomradekode, :utdanningsprogram::utdanning_program)
            on conflict (programomradekode) do update set
                navn = excluded.navn,
                utdanningsprogram = excluded.utdanningsprogram
        """.trimIndent()

        val params = mapOf(
            "navn" to programomrade.navn,
            "programomradekode" to programomrade.programomradekode,
            "utdanningsprogram" to programomrade.utdanningsprogram?.name,
        )

        queryOf(query, params).asExecute.runWithSession(session)
    }

    private fun saveUtdanning(session: TransactionalSession, utdanning: Utdanning) {
        @Language("PostgreSQL")
        val getIdForProgramomradeQuery = """
            select id from utdanning_programomrade where programomradekode = :programomradekode
        """.trimIndent()

        val programomradeId =
            queryOf(getIdForProgramomradeQuery, mapOf("programomradekode" to utdanning.utdanningslop.first()))
                .map { it.uuid("id") }
                .asSingle
                .runWithSession(session)

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
        ).asExecute.runWithSession(session)

        utdanning.nusKodeverk.forEach { nus ->
            queryOf(
                nuskodeInnholdInsertQuery,
                mapOf("title" to nus.navn, "nus_kode" to nus.kode),
            ).asExecute.runWithSession(session)

            queryOf(
                nusKodeKoblingforUtdanningQuery,
                mapOf("utdanning_id" to utdanning.utdanningId, "nus_kode_id" to nus.kode),
            ).asExecute.runWithSession(session)
        }
    }
}

private fun resolveRelevantUtdanninger(utdanninger: List<UtdanningNoProgramomraade>): Pair<List<Programomrade>, List<Utdanning>> {
    return utdanninger
        .partition { it.sluttkompetanse == null && it.utdanningId == null && it.utdanningslop.size == 1 }
        .let { (programomrader, utdanninger) ->
            val relevantProgramomrader = programomrader.map { toProgramomrade(it) }

            val relevantUtdanninger = utdanninger
                .filter { it.nusKodeverk.isNotEmpty() }
                .map { sanitizeUtdanning(it) }

            Pair(relevantProgramomrader, relevantUtdanninger)
        }
}

private fun toProgramomrade(utdanning: UtdanningNoProgramomraade): Programomrade {
    val navn = utdanning.navn.replace("^Vg1 ".toRegex(), "")
    val utdanningsprogram = when (utdanning.utdanningsprogram) {
        UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG -> Utdanningsprogram.YRKESFAGLIG
        UtdanningNoProgramomraade.Utdanningsprogram.STUDIEFORBEREDENDE -> Utdanningsprogram.STUDIEFORBEREDENDE
        null -> null
    }
    return Programomrade(navn, emptyList(), utdanning.programomradekode, utdanningsprogram)
}

private fun sanitizeUtdanning(utdanning: UtdanningNoProgramomraade): Utdanning {
    return Utdanning(
        navn = utdanning.navn.replace(" \\(opplæring i bedrift\\)\$".toRegex(), ""),
        programomradekode = utdanning.programomradekode,
        utdanningId = requireNotNull(utdanning.utdanningId) {
            "klarte ikke lese utdanningId for utdanning=$utdanning"
        },
        utdanningsprogram = when (utdanning.utdanningsprogram) {
            UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG -> Utdanningsprogram.YRKESFAGLIG
            UtdanningNoProgramomraade.Utdanningsprogram.STUDIEFORBEREDENDE -> Utdanningsprogram.STUDIEFORBEREDENDE
            null -> throw IllegalArgumentException("utdanningsprogram mangler for utdanning=$utdanning")
        },
        sluttkompetanse = when (utdanning.sluttkompetanse) {
            UtdanningNoProgramomraade.Sluttkompetanse.Fagbrev -> Utdanning.Sluttkompetanse.FAGBREV
            UtdanningNoProgramomraade.Sluttkompetanse.Svennebrev -> Utdanning.Sluttkompetanse.SVENNEBREV
            UtdanningNoProgramomraade.Sluttkompetanse.Yrkeskompetanse -> Utdanning.Sluttkompetanse.YRKESKOMPETANSE
            UtdanningNoProgramomraade.Sluttkompetanse.Studiekompetanse -> Utdanning.Sluttkompetanse.STUDIEKOMPETANSE
            null -> null
        },
        aktiv = utdanning.aktiv,
        utdanningstatus = when (utdanning.utdanningstatus) {
            UtdanningNoProgramomraade.Status.KOMMENDE -> Utdanning.Status.KOMMENDE
            UtdanningNoProgramomraade.Status.GYLDIG -> Utdanning.Status.GYLDIG
            UtdanningNoProgramomraade.Status.UTGAAENDE -> Utdanning.Status.UTGAAENDE
            UtdanningNoProgramomraade.Status.UTGAATT -> Utdanning.Status.UTGAATT
        },
        utdanningslop = utdanning.utdanningslop,
        nusKodeverk = utdanning.nusKodeverk.map { NusKodeverk(navn = it.navn, kode = it.kode) },
    )
}
