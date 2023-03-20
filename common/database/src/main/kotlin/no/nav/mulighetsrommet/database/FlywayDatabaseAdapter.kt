package no.nav.mulighetsrommet.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.mulighetsrommet.slack_notifier.SlackNotifier
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class FlywayDatabaseAdapter(
    config: FlywayDatabaseConfig,
    private val slackNotifier: SlackNotifier? = null
) : DatabaseAdapter(config) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val flyway: Flyway

    init {
        flyway = Flyway
            .configure()
            .cleanDisabled(config.migrationConfig.cleanDisabled)
            .lockRetryCount(-1) // -1 = prøv for alltid https://flywaydb.org/documentation/configuration/parameters/lockRetryCount
            .configuration(
                mapOf(
                    // Disable transactional locks in order to support concurrent indexes
                    "flyway.postgresql.transactional.lock" to "false"
                )
            )
            .dataSource(config.jdbcUrl, config.user, config.password.value)
            .apply {
                config.schema?.let { schemas(it) }
            }
            .load()

        when (config.migrationConfig.strategy) {
            InitializationStrategy.Migrate -> {
                migrate()
            }

            InitializationStrategy.MigrateAsync -> runAsync {
                migrate()
            }

            InitializationStrategy.RepairAndMigrate -> {
                repair()
                migrate()
            }
        }
    }

    data class MigrationConfig(
        val cleanDisabled: Boolean = true,
        val strategy: InitializationStrategy = InitializationStrategy.Migrate
    )

    enum class InitializationStrategy {
        Migrate,
        MigrateAsync,
        RepairAndMigrate,
    }

    fun repair() {
        flyway.repair()
    }

    fun migrate() {
        flyway.migrate()
    }

    fun clean() {
        flyway.clean()
    }

    @OptIn(ExperimentalTime::class)
    private fun runAsync(run: suspend CoroutineScope.() -> Unit): Job {
        return CoroutineScope(Job()).launch {
            logger.info("Running async flyway task...")
            try {
                val time = measureTime {
                    run()
                }
                logger.info("Flyway task finished in ${time}ms")
            } catch (e: Throwable) {
                slackNotifier?.sendMessage("Async Flyway-migrering feilet. Sjekk med utviklerne på teamet om noen kjører en stor async migrering.")
                logger.warn("Flyway task was cancelled with exception", e)
                throw e
            }
        }
    }
}
