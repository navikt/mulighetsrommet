package no.nav.mulighetsrommet.test.extensions

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import no.nav.mulighetsrommet.arena.adapter.Database
import no.nav.mulighetsrommet.arena.adapter.DatabaseConfig

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
