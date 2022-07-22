package no.nav.mulighetsrommet.database.kotest.extensions

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig

class DatabaseListener(private val config: DatabaseConfig) : BeforeSpecListener, AfterSpecListener {
    private var delegate: Database? = null

    val db: Database
        get() = delegate ?: throw RuntimeException("Database has not yet been initialized")

    override suspend fun beforeSpec(spec: Spec) {
        delegate = Database(config)
    }

    override suspend fun afterSpec(spec: Spec) {
        delegate?.clean()
    }
}
