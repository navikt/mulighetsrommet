package no.nav.mulighetsrommet.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

class FlywayDatabaseAdapter(
    config: DatabaseConfig,
    strategy: InitializationStrategy,
) : DatabaseAdapter(config) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val flyway: Flyway

    init {
        flyway = Flyway
            .configure()
            .dataSource(config.jdbcUrl, config.user, config.password.value)
            .apply {
                config.schema?.let { schemas(it) }
            }
            .load()

        when (strategy) {
            InitializationStrategy.Migrate -> migrate()
            InitializationStrategy.MigrateAsync -> runAsync {
                migrate()
            }
        }
    }

    enum class InitializationStrategy {
        Migrate,
        MigrateAsync,
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
