package no.nav.mulighetsrommet

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCaseOrder
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.DatabaseConfig
import no.nav.mulighetsrommet.database.FlywayMigrationManager
import org.assertj.db.api.Assertions
import org.assertj.db.api.TableAssert
import org.assertj.db.type.Table
import kotlin.time.measureTime

class ApiDatabaseTestListener(private val config: DatabaseConfig) : BeforeSpecListener, AfterSpecListener {
    private var delegate: Database? = null

    private val flywayMigration: FlywayMigrationManager = FlywayMigrationManager(
        config = FlywayMigrationManager.MigrationConfig(cleanDisabled = false),
        slackNotifier = null,
    )

    val db: ApiDatabase
        get() {
            return delegate?.let { ApiDatabase(it) } ?: throw RuntimeException("Database has not yet been initialized")
        }

    override suspend fun beforeSpec(spec: Spec) {
        // It's not optimal to force a sequential test order, but since tests (for now) all share the same database
        // instance they can't be run in parallel
        spec.testOrder = TestCaseOrder.Sequential

        delegate = Database(config)

        flywayMigration.migrate(db.db)
    }

    override suspend fun afterSpec(spec: Spec) {
        truncateAll()
        delegate?.close()
    }

    fun assertThat(tableName: String): TableAssert {
        val table = Table(db.getDatasource(), tableName)
        return Assertions.assertThat(table)
    }

    inline fun <T> run(block: QueryContext.(TransactionalSession) -> T): T = with(db.db.session) {
        try {
            connection.begin()
            transactional = true
            val tx = TransactionalSession(connection, returnGeneratedKeys, autoGeneratedKeys, strict)
            val context = QueryContext(tx)
            val result = block.invoke(context, tx)
            connection.commit()
            return result
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            transactional = false
            close()
        }
    }

    inline fun <T> runAndRollback(block: QueryContext.(TransactionalSession) -> T): T = with(db.db.session) {
        try {
            connection.begin()
            transactional = true
            val tx = TransactionalSession(connection, returnGeneratedKeys, autoGeneratedKeys, strict)
            val context = QueryContext(tx)
            val result = block.invoke(context, tx)
            connection.rollback()
            return result
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            transactional = false
            close()
        }
    }

    fun truncateAll() {
        val time = measureTime {
            val tableNames =
                queryOf("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")
                    .map { it.string("table_name") }
                    .asList
                    .let { db.db.run(it) }
            tableNames.forEach {
                db.db.run(queryOf("truncate table $it restart identity cascade").asExecute)
            }
        }
        println("time ${time.inWholeMilliseconds}")
    }
}
