package no.nav.mulighetsrommet.database

import com.codahale.metrics.health.HealthCheckRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.*
import kotliquery.Session
import kotliquery.action.*
import java.io.Closeable
import java.sql.Array
import java.util.*
import javax.sql.DataSource

class Database(val config: DatabaseConfig) : Closeable {

    private val dataSource: HikariDataSource

    @PublishedApi
    internal val session: Session
        get() = sessionOf(dataSource, strict = true)

    init {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            config.schema?.let {
                schema = config.schema
            }
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = config.maximumPoolSize
            healthCheckRegistry = HealthCheckRegistry()

            config.googleCloudSqlInstance?.let {
                dataSourceProperties = Properties().apply {
                    setProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory")
                    setProperty("cloudSqlInstance", config.googleCloudSqlInstance)
                }
            }

            config.additionalConfig.invoke(this)

            validate()
        }
        dataSource = HikariDataSource(hikariConfig)
    }

    override fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }

    fun getDatasource(): DataSource {
        return dataSource
    }

    fun isHealthy(): Boolean {
        return (dataSource.healthCheckRegistry as? HealthCheckRegistry)
            ?.runHealthChecks()
            ?.all { it.value.isHealthy }
            ?: false
    }

    fun createArrayOf(arrayType: String, list: Collection<Any>): Array {
        return session {
            it.createArrayOf(arrayType, list)
        }
    }

    fun <T> run(query: NullableResultQueryAction<T>): T? {
        return session {
            it.run(query)
        }
    }

    fun <T> run(query: ListResultQueryAction<T>): List<T> {
        return session {
            it.run(query)
        }
    }

    fun run(query: ExecuteQueryAction): Boolean {
        return session {
            it.run(query)
        }
    }

    fun run(query: UpdateQueryAction): Int {
        return session {
            it.run(query)
        }
    }

    fun run(query: UpdateAndReturnGeneratedKeyQueryAction): Long? {
        return session {
            it.run(query)
        }
    }

    inline fun <T> session(operation: (Session) -> T): T = session.use { s ->
        operation(s)
    }

    inline fun <T> transaction(operation: (TransactionalSession) -> T): T = session.use { s ->
        s.transaction(operation)
    }
}
