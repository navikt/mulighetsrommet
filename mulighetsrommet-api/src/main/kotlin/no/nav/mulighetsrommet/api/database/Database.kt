package no.nav.mulighetsrommet.api.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.sessionOf
import no.nav.mulighetsrommet.api.DatabaseConfig
import org.flywaydb.core.Flyway

class Database(databaseConfig: DatabaseConfig) {

    private var db: HikariDataSource
    var flyway: Flyway
    var session: Session

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
        hikariConfig.validate()

        db = HikariDataSource(hikariConfig)

        session = sessionOf(db)
    }
}
