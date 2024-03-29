package no.nav.mulighetsrommet.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val hasMigrated = AtomicBoolean(false)

class FlywayDatabaseAdapter(
    val config: Config,
    private val slackNotifier: SlackNotifier? = null,
) : DatabaseAdapter(config) {

    data class Config(
        override val host: String,
        override val port: Int,
        override val name: String,
        override val schema: String?,
        override val user: String,
        override val password: Password,
        override val maximumPoolSize: Int,
        override val googleCloudSqlInstance: String? = null,
        val migrationConfig: MigrationConfig = MigrationConfig(),
    ) : DatabaseConfig

    data class MigrationConfig(
        val cleanDisabled: Boolean = true,
        val strategy: InitializationStrategy = InitializationStrategy.Migrate,
    )

    enum class InitializationStrategy {
        Migrate,
        MigrateAsync,
        RepairAndMigrate,
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private val flyway: Flyway = Flyway
        .configure()
        .cleanDisabled(config.migrationConfig.cleanDisabled)
        .lockRetryCount(-1) // -1 = prøv for alltid https://flywaydb.org/documentation/configuration/parameters/lockRetryCount
        .configuration(
            mapOf(
                // Disable transactional locks in order to support concurrent indexes
                "flyway.postgresql.transactional.lock" to "false",
            ),
        )
        .dataSource(config.jdbcUrl, config.user, config.password.value)
        .apply {
            config.schema?.let { schemas(it) }
        }
        .load()

    init {
        if (!hasMigrated.get()) {
            hasMigrated.set(true)
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
    }

    fun repair() {
        flyway.repair()
    }

    fun migrate() {
        flyway.migrate()
    }

    @OptIn(ExperimentalTime::class)
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
