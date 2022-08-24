package no.nav.mulighetsrommet.database

import com.codahale.metrics.health.HealthCheckRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.action.*
import kotliquery.sessionOf
import kotliquery.using
import org.flywaydb.core.Flyway
import java.sql.Array

class Database(config: DatabaseConfig) {
    private val flyway: Flyway

    val dataSource: HikariDataSource

    private val session: Session
        get() = sessionOf(dataSource)

    init {
        flyway = Flyway
            .configure()
            .dataSource(config.jdbcUrl, config.user, config.password.value)
            .apply {
                config.schema?.let { schemas(it) }
            }
            .load()

        flyway.migrate()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            config.schema?.let {
                schema = config.schema
            }
            driverClassName = "org.postgresql.Driver"
            username = config.user
            password = config.password.value
            maximumPoolSize = config.maximumPoolSize
            healthCheckRegistry = HealthCheckRegistry()
            validate()
        }
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

    fun <T> transaction(operation: (TransactionalSession) -> T): T {
        return using(session) {
            it.transaction(operation)
        }
    }
}
