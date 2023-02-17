package no.nav.mulighetsrommet.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

class FlywayDatabaseAdapter(
    config: FlywayDatabaseConfig,
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

    private fun runAsync(run: suspend CoroutineScope.() -> Unit): Job {
        return CoroutineScope(Job()).launch {
            logger.info("Running async flyway task...")
            try {
                val time = measureTimeMillis {
                    run()
                }
                logger.info("Flyway task finished in ${time}ms")
            } catch (e: Throwable) {
                logger.warn("Flyway task was cancelled with exception", e)
                throw e
            }
        }
    }
}
