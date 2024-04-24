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
        ForceClearRepeatableAndMigrate,
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

                InitializationStrategy.ForceClearRepeatableAndMigrate -> {
                    forceRepeatableMigrations()
                    migrate()
                }
            }
        }
    }

    private fun repair() {
        flyway.repair()
    }

    private fun migrate() {
        flyway.migrate()
    }

    /**
     * Vi opplever stadig at repeatable views ikke håndteres skikkelig når applikasjonen kjøres lokalt og at vi ender
     * opp med at databasen mangler views spesifisert i repeatable migrasjoner.
     *
     * Ved å kjøre denne funksjonen før vi kjører migrasjoner så fjerner vi alle entries av repeatable migrasjoner fra
     * flyway_schema_history, som igjen trigger flyway til å _alltid_ kjøre alle repetable migrasjoner på nytt.
     */
    private fun forceRepeatableMigrations() {
        @Language("PostgreSQL")
        val deleteRepeatableEntry = """
            delete from flyway_schema_history
            where installed_rank = ? and checksum = ? and version is null
        """.trimIndent()

        useSession { session ->
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
