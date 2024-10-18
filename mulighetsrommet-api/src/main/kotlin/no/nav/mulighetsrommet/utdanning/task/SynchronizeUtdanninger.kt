package no.nav.mulighetsrommet.utdanning.task

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.client.UtdanningNoProgramomraade
import no.nav.mulighetsrommet.utdanning.db.*
import no.nav.mulighetsrommet.utdanning.model.NusKodeverk
import no.nav.mulighetsrommet.utdanning.model.Utdanning
import no.nav.mulighetsrommet.utdanning.model.Utdanningsprogram
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramType
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.jvm.optionals.getOrNull

class SynchronizeUtdanninger(
    config: Config,
    private val db: Database,
    private val utdanningClient: UtdanningClient,
    private val slack: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val utdanningRepository = UtdanningRepository(db)

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

    val task: RecurringTask<Void> = Tasks.recurring(javaClass.name, config.toSchedule())
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
            programomrader.forEach { utdanningRepository.upsertPrograomomrade(tx, it) }
            utdanninger.forEach { utdanningRepository.upsertUtdanning(tx, it) }
        }
    }
}

private fun resolveRelevantUtdanninger(utdanninger: List<UtdanningNoProgramomraade>): Pair<List<Utdanningsprogram>, List<Utdanning>> {
    return utdanninger
        .partition { it.sluttkompetanse == null && it.utdanningId == null && it.utdanningslop.size == 1 }
        .let { (programomrader, utdanninger) ->
            val utdanningssprogrammer = programomrader
                .filter { it.utdanningsprogram == UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG }
                .map { toUtdanningsprogram(it) }

            val relevanteUtdanninger = utdanninger
                .filter { it.nusKodeverk.isNotEmpty() }
                .map { sanitizeUtdanning(it) }

            Pair(utdanningssprogrammer, relevanteUtdanninger)
        }
}

private fun toUtdanningsprogram(utdanning: UtdanningNoProgramomraade): Utdanningsprogram {
    val navn = utdanning.navn.replace("^Vg\\d ".toRegex(), "")
    val utdanningsprogram = when (utdanning.utdanningsprogram) {
        UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG -> UtdanningsprogramType.YRKESFAGLIG
        else -> throw IllegalArgumentException("Utdanningsprogram må være yrkesfaglig, '${utdanning.utdanningsprogram}' ble mottatt")
    }

    val nusKoder = utledNusKoder(utdanning.programomradekode)

    return Utdanningsprogram(
        navn = navn,
        nusKoder = nusKoder,
        programomradekode = utdanning.programomradekode,
        type = utdanningsprogram,
    )
}

/**
 * Nus-kodene har blitt bestemt av datavarehuset.
 * Fag/DVH må være involvert hvis/når det oppstår nye programområder som skal støtes.
 */
private fun utledNusKoder(programomradekode: String): List<String> {
    val nusKoderMapping = mapOf(
        "BABAT1----" to listOf("3571"),
        "ELELE1----" to listOf("3551"),
        "FDFBI1----" to listOf("3165"),
        "HSHSF1----" to listOf("3699"),
        "DTDTH1----" to listOf("3169"),
        "IMIKM1----" to listOf("3541"),
        "NANAB1----" to listOf("3799"),
        "RMRMF1----" to listOf("3581"),
        "SRSSR1----" to listOf("3429"),
        "TPTIP1----" to listOf("3559"),
    )

    return requireNotNull(nusKoderMapping[programomradekode]) {
        "Mangler nuskoder for programområde $programomradekode"
    }
}

private fun sanitizeUtdanning(utdanning: UtdanningNoProgramomraade): Utdanning {
    return Utdanning(
        navn = utdanning.navn.replace(" \\(opplæring i bedrift\\)\$".toRegex(), ""),
        programomradekode = utdanning.programomradekode,
        utdanningId = requireNotNull(utdanning.utdanningId) {
            "klarte ikke lese utdanningId for utdanning=$utdanning"
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
