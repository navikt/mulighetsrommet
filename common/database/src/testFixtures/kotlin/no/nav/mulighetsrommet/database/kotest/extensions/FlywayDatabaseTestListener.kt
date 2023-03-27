package no.nav.mulighetsrommet.database.kotest.extensions

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCaseOrder
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import org.assertj.db.api.Assertions
import org.assertj.db.api.TableAssert
import org.assertj.db.type.Table

class FlywayDatabaseTestListener(private val config: FlywayDatabaseAdapter.Config) : BeforeSpecListener, AfterSpecListener {
    private var delegate: FlywayDatabaseAdapter? = null

    val db: FlywayDatabaseAdapter
        get() = delegate ?: throw RuntimeException("Database has not yet been initialized")

    override suspend fun beforeSpec(spec: Spec) {
        // It's not optimal to force a sequential test order, but since tests (for now) all share the same database
        // instance they can't be run in parallel
        spec.testOrder = TestCaseOrder.Sequential

        delegate = FlywayDatabaseAdapter(config, slackNotifier = null)
    }

    override suspend fun afterSpec(spec: Spec) {
        delegate?.clean()
    }

    fun assertThat(tableName: String): TableAssert {
        val table = Table(db.getDatasource(), tableName)
        return Assertions.assertThat(table)
    }
}
