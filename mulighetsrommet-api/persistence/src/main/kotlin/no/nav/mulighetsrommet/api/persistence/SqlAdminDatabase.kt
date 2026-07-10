package no.nav.mulighetsrommet.api.persistence

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.database.Database

class SqlAdminDatabase(
    private val db: Database,
    private val topics: OutboxTopics,
) : AdminDatabase {
    override fun <T> session(block: QueryContext.() -> T): T = db.session { session -> SqlQueryContext(session, topics).block() }

    override fun <T> transaction(block: QueryContext.() -> T): T = db.transaction { session -> SqlQueryContext(session, topics).block() }

    override suspend fun <T> suspendSession(block: suspend QueryContext.() -> T): T {
        val ctx = currentCoroutineContext()
        return db.session { session -> runBlocking(ctx) { SqlQueryContext(session, topics).block() } }
    }

    override suspend fun <T> suspendTransaction(block: suspend QueryContext.() -> T): T {
        val ctx = currentCoroutineContext()
        return db.transaction { session -> runBlocking(ctx) { SqlQueryContext(session, topics).block() } }
    }
}
