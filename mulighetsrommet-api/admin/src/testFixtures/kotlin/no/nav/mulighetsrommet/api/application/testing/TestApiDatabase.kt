package no.nav.mulighetsrommet.api.application.testing

import no.nav.mulighetsrommet.api.application.ApiDatabase
import no.nav.mulighetsrommet.api.application.QueryContext

class TestApiDatabase(val ctx: TestQueryContext = TestQueryContext()) : ApiDatabase {
    val queries: QueryContext.Queries get() = ctx.queries
    val repository: QueryContext.Repositories get() = ctx.repository

    override fun <T> session(block: QueryContext.() -> T): T = block(ctx)

    override fun <T> transaction(block: QueryContext.() -> T): T = block(ctx)

    override suspend fun <T> suspendSession(block: suspend QueryContext.() -> T): T = block(ctx)

    override suspend fun <T> suspendTransaction(block: suspend QueryContext.() -> T): T = block(ctx)
}
