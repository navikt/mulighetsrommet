package no.nav.mulighetsrommet.arena.adapter

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class Database(databaseConfig: DatabaseConfig) {

    private val logger = LoggerFactory.getLogger(Database::class.java)
    private var flyway: Flyway
    val dataSource: HikariDataSource
    val session: Session

    init {
        val jdbcUrl = "jdbc:postgresql://${databaseConfig.host}:${databaseConfig.port}/${databaseConfig.name}"

        logger.debug("Initializing Database")
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.username = databaseConfig.user
        hikariConfig.password = databaseConfig.password.value
        hikariConfig.maximumPoolSize = 3

        hikariConfig.validate()
        dataSource = HikariDataSource(hikariConfig)
        session = sessionOf(dataSource)

        logger.debug("Start flyway migrations")
        flyway = Flyway.configure().dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value).load()
        flyway.migrate()
    }

    fun persistKafkaEvent(topic: String, key: String, payload: String) {
        @Language("PostgreSQL")
        val query = """
            insert into events(topic, key, payload)
            values (?, ?, ?::jsonb)
            on conflict (topic, key)
            do update set
                payload = excluded.payload
        """.trimIndent()
        session.run(queryOf(query, topic, key, payload).asUpdate)
        logger.debug("Persisted kafka event: $topic:$key")
    }
}
