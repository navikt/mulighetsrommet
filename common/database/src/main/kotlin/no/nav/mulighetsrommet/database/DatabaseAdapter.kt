package no.nav.mulighetsrommet.database

import com.codahale.metrics.health.HealthCheckRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.action.*
import kotliquery.sessionOf
import kotliquery.using
import java.sql.Array
import java.util.*
import javax.sql.DataSource

open class DatabaseAdapter(config: DatabaseConfig) : Database {
    private val dataSource: HikariDataSource

    private val session: Session
        get() = sessionOf(dataSource)

    init {
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

            config.googleCloudSqlInstance?.let {
                dataSourceProperties = Properties().apply {
                    setProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory")
                    setProperty("cloudSqlInstance", config.googleCloudSqlInstance)
                }
            }

            validate()
        }
        dataSource = HikariDataSource(hikariConfig)
    }

    override fun getDatasource(): DataSource {
        return dataSource
    }

    override fun isHealthy(): Boolean {
        return (dataSource.healthCheckRegistry as? HealthCheckRegistry)
            ?.runHealthChecks()
            ?.all { it.value.isHealthy }
            ?: false
    }

    override fun createArrayOf(arrayType: String, list: Collection<Any>): Array {
        return using(session) {
            it.createArrayOf(arrayType, list)
        }
    }

    override fun createTextArray(list: Collection<String>): Array {
        return createArrayOf("text", list)
    }

    override fun <T> run(query: NullableResultQueryAction<T>): T? {
        return using(session) {
            it.run(query)
        }
    }

    override fun <T> run(query: ListResultQueryAction<T>): List<T> {
        return using(session) {
            it.run(query)
        }
    }

    override fun run(query: ExecuteQueryAction): Boolean {
        return using(session) {
            it.run(query)
        }
    }

    override fun run(query: UpdateQueryAction): Int {
        return using(session) {
            it.run(query)
        }
    }

    override fun run(query: UpdateAndReturnGeneratedKeyQueryAction): Long? {
        return using(session) {
            it.run(query)
        }
    }

    override fun <T> transaction(operation: (TransactionalSession) -> T): T {
        return using(session) {
            it.transaction(operation)
        }
    }
}
