package no.nav.mulighetsrommet.api

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseIfNotExists
import no.nav.mulighetsrommet.database.kotest.extensions.dropDatabaseIfExists
import org.slf4j.LoggerFactory

object KotestProjectConfig : AbstractProjectConfig(), BeforeProjectListener, AfterProjectListener {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun beforeProject() {
        val config = createDatabaseTestConfig()
        log.info("Creating test database '${config.name}'...")
        createDatabaseIfNotExists(config)
    }

    override suspend fun afterProject() {
        val config = createDatabaseTestConfig()
        log.info("Dropping test database '${config.name}'...")
        dropDatabaseIfExists(config)
    }
}
