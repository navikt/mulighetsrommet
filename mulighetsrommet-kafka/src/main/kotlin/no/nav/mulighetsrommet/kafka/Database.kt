package no.nav.mulighetsrommet.kafka

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.JsonElement
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

class Database(databaseConfig: DatabaseConfig) {

    private val logger = LoggerFactory.getLogger(Database::class.java)
    private var flyway: Flyway
    private var db: HikariDataSource
    private var session: Session

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

        // TODO: Flytt ut til CI etterhvert
        flyway = Flyway.configure().dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value).load()
        flyway.migrate()
    }

    fun persistKafkaEvent(topic: String, key: String, offset: Long, payload: String) {
        val query = "insert into events(topic, key, \"offset\", payload) values(?, ?, ?, ? ::jsonb)"
        val result = session.run(queryOf(query, topic, key, offset, payload).asUpdate)
        if (result > 0) logger.debug("persisted kafka event ($topic, $key, $offset)")
    }

    data class Event(
        val id: Int?,
        val topic: String,
        val key: String,
        val offset: Long,
        val payload: JsonElement
    )
}
