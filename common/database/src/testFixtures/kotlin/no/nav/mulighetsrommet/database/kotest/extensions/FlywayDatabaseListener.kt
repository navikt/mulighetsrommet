package no.nav.mulighetsrommet.database.kotest.extensions

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter

class FlywayDatabaseListener(private val config: DatabaseConfig) : BeforeSpecListener, AfterSpecListener {
    private var delegate: FlywayDatabaseAdapter? = null

    val db: FlywayDatabaseAdapter
        get() = delegate ?: throw RuntimeException("Database has not yet been initialized")

    override suspend fun beforeSpec(spec: Spec) {
        delegate = FlywayDatabaseAdapter(config, FlywayDatabaseAdapter.InitializationStrategy.Migrate)
    }

    override suspend fun afterSpec(spec: Spec) {
        delegate?.clean()
    }
}
