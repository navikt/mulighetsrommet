package no.nav.mulighetsrommet.database.kotest.extensions

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.BeforeProjectListener
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory

/**
 * Extension til Kotest som oppretter en database før testene i et Kotest-prosjekt kjører
 * og rydder opp (sletter) databasen etter at alle testene har kjørt.
 */
open class CreateDatabaseTestListener(private val config: DatabaseConfig) :
    AbstractProjectConfig(),
    BeforeProjectListener,
    AfterProjectListener {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun beforeProject() {
        log.info("Creating test database '${config.getDatabaseName()}'...")

        try {
            Database(createDatabaseConfig()).use {
                it.run(queryOf("create database \"${config.getDatabaseName()}\"").asExecute)
            }
        } catch (e: PSQLException) {
            // This error code is expected (https://www.postgresql.org/docs/8.2/errcodes-appendix.html)
            val errorCodeDuplicateDatabase = "42P04"
            if (e.sqlState != errorCodeDuplicateDatabase) {
                // Otherwise rethrow
                throw e
            }
        }
    }

    override suspend fun afterProject() {
        log.info("Dropping test database '${config.getDatabaseName()}'...")

        Database(createDatabaseConfig()).use {
            it.run(queryOf("drop database if exists \"${config.getDatabaseName()}\"").asExecute)
        }
    }
}
