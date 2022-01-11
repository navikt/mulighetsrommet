package no.nav.mulighetsrommet.api.database

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.config.HoconApplicationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseFactory {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val dbUser = appConfig.property("ktor.db.user").getString()
    private val dbPassword = appConfig.property("ktor.db.password").getString()
    private val dbHost = appConfig.property("ktor.db.host").getString()
    private val dbName = appConfig.property("ktor.db.name").getString()
    private val dbPort = appConfig.property("ktor.db.port").getString()
    private val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

    private var flyway: Flyway
    private lateinit var dataSource: HikariDataSource

    init {
        Database.connect(hikari())
        flyway = Flyway.configure().dataSource(jdbcUrl, dbUser, dbPassword).load()
        flyway.migrate()
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }

    fun migrateDatabase(): Int {
        return flyway.migrate().migrationsExecuted
    }

    fun cleanDatabase(): ArrayList<String>? {
        return flyway.clean().schemasCleaned
    }

    // TODO: Finn en løsning på å sjekke active connection. Ting bare tryner her uansett hva jeg tester.
    // fun isConnectionActive() = !dataSource.connection.isClosed

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = jdbcUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        dataSource = HikariDataSource(config)
        return dataSource
    }
}
