package no.nav.mulighetsrommet.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotliquery.queryOf
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.measureTime

private val hasMigrated = AtomicBoolean(false)

class FlywayMigrationManager(
    val config: MigrationConfig,
    private val slackNotifier: SlackNotifier? = null,
) {

    data class MigrationConfig(
        val cleanDisabled: Boolean = true,
        val strategy: InitializationStrategy = InitializationStrategy.Migrate,
    )

    enum class InitializationStrategy {
        Migrate,
        MigrateAsync,
        RepairAndMigrate,
        ForceClearRepeatableAndMigrate,
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun migrate(database: Database) {
        if (hasMigrated.get()) {
            return
        }

        val flyway: Flyway = Flyway
            .configure()
            .cleanDisabled(config.cleanDisabled)
            .lockRetryCount(-1) // -1 = prøv for alltid https://flywaydb.org/documentation/configuration/parameters/lockRetryCount
            .configuration(
                mapOf(
                    // Disable transactional locks in order to support concurrent indexes
                    "flyway.postgresql.transactional.lock" to "false",
                ),
            )
            .dataSource(database.getDatasource())
            .apply {
                database.config.schema?.let { schemas(it) }
            }
            .load()

        when (config.strategy) {
            InitializationStrategy.Migrate -> {
                flyway.migrate()
            }

            InitializationStrategy.MigrateAsync -> runAsync {
                flyway.migrate()
            }

            InitializationStrategy.RepairAndMigrate -> {
                flyway.repair()
                flyway.migrate()
            }

            InitializationStrategy.ForceClearRepeatableAndMigrate -> {
                forceRepeatableMigrations(database, flyway)
                flyway.migrate()
            }
        }

        hasMigrated.set(true)
    }

    /**
     * Vi opplever stadig at repeatable views ikke håndteres skikkelig når applikasjonen kjøres lokalt og at vi ender
     * opp med at databasen mangler views spesifisert i repeatable migrasjoner.
     *
     * Ved å kjøre denne funksjonen før vi kjører migrasjoner så fjerner vi alle entries av repeatable migrasjoner fra
     * flyway_schema_history, som igjen trigger flyway til å _alltid_ kjøre alle repetable migrasjoner på nytt.
     */
    private fun forceRepeatableMigrations(database: Database, flyway: Flyway) {
        @Language("PostgreSQL")
        val deleteRepeatableEntry = """
            delete from flyway_schema_history
            where installed_rank = ? and checksum = ? and version is null
        """.trimIndent()

        database.useSession { session ->
            flyway.info().all().filter { !it.isVersioned }.forEach {
                queryOf(deleteRepeatableEntry, it.installedRank, it.checksum).asExecute.runWithSession(session)
            }
        }
    }

    private fun runAsync(run: suspend CoroutineScope.() -> Unit): Job {
        return CoroutineScope(Job()).launch {
            logger.info("Running async flyway task...")
            try {
                val time = measureTime {
                    run()
                }
                logger.info("Flyway task finished in $time")
            } catch (e: Throwable) {
                slackNotifier?.sendMessage(
                    """
                    Async Flyway-migrering feilet.
                    Sjekk med utviklerne på teamet om noen kjører en stor async migrering.
                    """.trimIndent(),
                )
                logger.warn("Flyway task was cancelled with exception", e)
                throw e
            }
        }
    }
}
