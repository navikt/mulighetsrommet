package no.nav.mulighetsrommet.database

import com.codahale.metrics.health.HealthCheckRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.action.*
import kotliquery.sessionOf
import java.io.Closeable
import java.sql.Array
import java.util.*
import javax.sql.DataSource

class Database(val config: DatabaseConfig) : Closeable {

    private val dataSource: HikariDataSource

//    @PublishedApi
//    internal val session: Session
//        get() = sessionOf(dataSource, strict = true)

    val session: Session
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

            config.additinalConfig.invoke(this)

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

    fun <T> useSession(operation: (Session) -> T): T {
        return session.use {
            operation(it)
        }
    }

    fun createArrayOf(arrayType: String, list: Collection<Any>): Array {
        return useSession {
            it.createArrayOf(arrayType, list)
        }
    }

    fun createTextArray(list: Collection<String>): Array {
        return createArrayOf("text", list)
    }

    fun createUuidArray(list: Collection<UUID>): Array {
        return createArrayOf("uuid", list)
    }

    fun createIntArray(list: Collection<Int>): Array {
        return createArrayOf("integer", list)
    }

    fun <T> run(query: NullableResultQueryAction<T>): T? {
        return useSession {
            it.run(query)
        }
    }

    fun <T> run(query: ListResultQueryAction<T>): List<T> {
        return useSession {
            it.run(query)
        }
    }

    fun run(query: ExecuteQueryAction): Boolean {
        return useSession {
            it.run(query)
        }
    }

    fun run(query: UpdateQueryAction): Int {
        return useSession {
            it.run(query)
        }
    }

    fun run(query: UpdateAndReturnGeneratedKeyQueryAction): Long? {
        return useSession {
            it.run(query)
        }
    }

    fun <T> transaction(operation: (TransactionalSession) -> T): T {
        return session.use {
            it.transaction(operation)
        }
    }

    // Dette er basically en kopi av session.transaction metoden bare i en suspend variant
    suspend fun <T> transactionSuspend(operation: suspend (TransactionalSession) -> T): T {
        return session.use {
            it.transaction { tx -> operation(tx) }
        }
    }

    inline fun <T> session(operation: Session.() -> T): T {
        return session.use { operation(it) }
    }

    suspend fun <T> sessionSuspend(operation: suspend Session.() -> T): T {
        return session.use { operation(it) }
    }

    inline fun <T> tx(operation: TransactionalSession.() -> T): T {
        return session.use {
            it.transaction(operation)
        }
    }
}
