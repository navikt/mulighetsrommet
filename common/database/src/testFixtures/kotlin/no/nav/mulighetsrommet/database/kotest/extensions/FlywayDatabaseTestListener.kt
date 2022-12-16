package no.nav.mulighetsrommet.database.kotest.extensions

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.database.FlywayDatabaseConfig
import org.assertj.db.api.Assertions
import org.assertj.db.api.TableAssert
import org.assertj.db.type.Table

class FlywayDatabaseTestListener(private val config: FlywayDatabaseConfig) : BeforeSpecListener, AfterSpecListener {
    private var delegate: FlywayDatabaseAdapter? = null

    val db: FlywayDatabaseAdapter
        get() = delegate ?: throw RuntimeException("Database has not yet been initialized")

    override suspend fun beforeSpec(spec: Spec) {
        delegate = FlywayDatabaseAdapter(config)
    }

    override suspend fun afterSpec(spec: Spec) {
        delegate?.clean()
    }

    fun assertThat(tableName: String): TableAssert {
        val table = Table(db.getDatasource(), tableName)
        return Assertions.assertThat(table)
    }
}
