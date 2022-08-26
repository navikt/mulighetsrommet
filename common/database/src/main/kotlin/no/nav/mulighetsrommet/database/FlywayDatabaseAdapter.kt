package no.nav.mulighetsrommet.database

import org.flywaydb.core.Flyway

class FlywayDatabaseAdapter(config: DatabaseConfig) : DatabaseAdapter(config) {
    private val flyway: Flyway

    init {
        flyway = Flyway
            .configure()
            .dataSource(config.jdbcUrl, config.user, config.password.value)
            .apply {
                config.schema?.let { schemas(it) }
            }
            .load()

        flyway.migrate()
    }

    fun clean() {
        flyway.clean()
    }
}
