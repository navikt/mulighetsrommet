package no.nav.mulighetsrommet.database.kotest.extensions

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import no.nav.mulighetsrommet.database.DatabaseConfig
import org.slf4j.LoggerFactory

open class CreateDatabaseTestListener(private val config: DatabaseConfig) :
    AbstractProjectConfig(),
    BeforeProjectListener,
    AfterProjectListener {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun beforeProject() {
        log.info("Creating test database '${config.name}'...")
        createDatabaseIfNotExists(config)
    }

    override suspend fun afterProject() {
        log.info("Dropping test database '${config.name}'...")
        dropDatabaseIfExists(config)
    }
}
