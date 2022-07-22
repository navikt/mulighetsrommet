package no.nav.mulighetsrommet.api.database

import com.codahale.metrics.health.HealthCheckRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.action.ExecuteQueryAction
import kotliquery.action.ListResultQueryAction
import kotliquery.action.NullableResultQueryAction
import kotliquery.action.UpdateAndReturnGeneratedKeyQueryAction
import kotliquery.action.UpdateQueryAction
import kotliquery.sessionOf
import kotliquery.using
import no.nav.mulighetsrommet.api.DatabaseConfig
import org.flywaydb.core.Flyway
import java.sql.Array

class Database(databaseConfig: DatabaseConfig) {

    val dataSource: HikariDataSource
    val flyway: Flyway
    private val session: Session
        get() = sessionOf(dataSource)

    init {
        val jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.name}"

        flyway = Flyway
            .configure()
            .dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value)
            .apply {
                databaseConfig.schema?.let { schemas(it) }
            }
            .load()

        flyway.migrate()

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        databaseConfig.schema?.let { hikariConfig.schema = databaseConfig.schema }
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.username = databaseConfig.user
        hikariConfig.password = databaseConfig.password.value
        hikariConfig.maximumPoolSize = 3
        hikariConfig.healthCheckRegistry = HealthCheckRegistry()

        hikariConfig.validate()

        dataSource = HikariDataSource(hikariConfig)
    }

    fun clean() {
        flyway.clean()
    }

    fun runHealthChecks(): Boolean {
        return (dataSource.healthCheckRegistry as? HealthCheckRegistry)?.runHealthChecks()?.all { it.value.isHealthy }
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

    fun run(query: ExecuteQueryAction) {
        using(session) {
            it.run(query)
        }
    }

    fun run(query: UpdateQueryAction) {
        using(session) {
            it.run(query)
        }
    }

    fun run(query: UpdateAndReturnGeneratedKeyQueryAction) {
        using(session) {
            it.run(query)
        }
    }

}
