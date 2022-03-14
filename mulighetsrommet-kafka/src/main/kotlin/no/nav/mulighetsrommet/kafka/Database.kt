package no.nav.mulighetsrommet.kafka

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.flywaydb.core.Flyway

class Database(databaseConfig: DatabaseConfig) {

    private var flyway: Flyway
    private var db: HikariDataSource

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

        flyway = Flyway.configure().dataSource(jdbcUrl, databaseConfig.user, databaseConfig.password.value).load()
        flyway.migrate()
    }

    fun testSelect() {
        val query = "select * from events"
        var events: MutableList<Event>? = null
        db.connection.use { con ->
            con.prepareStatement(query).use { pst ->
                pst.executeQuery().use { rs ->
                    events = mutableListOf()
                    var event: Event
                    while (rs.next()) {
                        event = Event(
                            rs.getInt("id"),
                            rs.getString("topic"),
                            rs.getString("key"),
                            rs.getLong("offset"),
                            Json.parseToJsonElement(rs.getString("payload"))
                        )
                        events!!.add(event)
                    }
                }
            }
        }
        events?.forEach {
            println(it.id)
        }
    }

    data class Event(
        val id: Int?,
        val topic: String,
        val key: String,
        val offset: Long,
        val payload: JsonElement
    )
}
