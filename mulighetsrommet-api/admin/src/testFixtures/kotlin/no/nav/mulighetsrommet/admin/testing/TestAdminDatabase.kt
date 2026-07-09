package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext

class TestAdminDatabase(private val ctx: TestQueryContext = TestQueryContext()) : AdminDatabase {
    val queries: QueryContext.Queries = ctx.queries
    val repository: QueryContext.Repositories = ctx.repository
    val outbox: QueryContext.Outbox = ctx.outbox

    override fun <T> session(block: QueryContext.() -> T): T = block(ctx)

    override fun <T> transaction(block: QueryContext.() -> T): T = block(ctx)

    override suspend fun <T> suspendSession(block: suspend QueryContext.() -> T): T = block(ctx)

    override suspend fun <T> suspendTransaction(block: suspend QueryContext.() -> T): T = block(ctx)
}
