package no.nav.mulighetsrommet.api.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.sessionOf
import no.nav.mulighetsrommet.api.DatabaseConfig
import org.flywaydb.core.Flyway

class Database(databaseConfig: DatabaseConfig) {

    private var flyway: Flyway
    private var db: HikariDataSource
    var session: Session

    init {
        val jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.name}"
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.username = databaseConfig.user
        hikariConfig.password = databaseConfig.password.value
        hikariConfig.maximumPoolSize = 3

        hikariConfig.validate()

        db = HikariDataSource(hikariConfig)
        session = sessionOf(db)

        flyway = Flyway.configure().dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value).load()
        flyway.migrate()
    }
}
