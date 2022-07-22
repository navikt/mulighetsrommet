package no.nav.mulighetsrommet.arena.adapter

import com.codahale.metrics.health.HealthCheckRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.action.*
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.sql.Array

class Database(databaseConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(Database::class.java)

    private val flyway: Flyway

    val dataSource: HikariDataSource

    private val session: Session
        get() = sessionOf(dataSource)

    init {
        val jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.name}"

        flyway = Flyway
            .configure()
            .dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value)
            .load()

        logger.debug("Start flyway migrations")
        flyway.migrate()

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        databaseConfig.schema?.let { hikariConfig.schema = databaseConfig.schema }
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.username = databaseConfig.user
        hikariConfig.password = databaseConfig.password.value
        hikariConfig.maximumPoolSize = databaseConfig.maximumPoolSize
        hikariConfig.healthCheckRegistry = HealthCheckRegistry()
        hikariConfig.validate()

        logger.debug("Initializing database connection pool")
        dataSource = HikariDataSource(hikariConfig)
    }

    fun clean() {
        flyway.clean()
    }

    fun isHealthy(): Boolean {
        return (dataSource.healthCheckRegistry as? HealthCheckRegistry)
            ?.runHealthChecks()
            ?.all { it.value.isHealthy }
            ?: false
    }

    fun createArrayOf(arrayType: String, list: Collection<Any>): Array {
        return using(session) {
            it.createArrayOf(arrayType, list)
        }
    }

    fun <T> run(query: NullableResultQueryAction<T>): T? {
        return using(session) {
            it.run(query)
        }
    }

    fun <T> run(query: ListResultQueryAction<T>): List<T> {
        return using(session) {
            it.run(query)
        }
    }

    fun run(query: ExecuteQueryAction): Boolean {
        return using(session) {
            it.run(query)
        }
    }

    fun run(query: UpdateQueryAction): Int {
        return using(session) {
            it.run(query)
        }
    }

    fun run(query: UpdateAndReturnGeneratedKeyQueryAction): Long? {
        return using(session) {
            it.run(query)
        }
    }
}
