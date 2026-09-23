package no.nav.mulighetsrommet.api.veilederflate.testing

import no.nav.mulighetsrommet.api.veilederflate.QueryContext
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateDatabase

class TestVeilederflateDatabase(private val ctx: TestQueryContext = TestQueryContext()) : VeilederflateDatabase {
    val queries: QueryContext.Queries = ctx.queries
    val repository: QueryContext.Repositories = ctx.repository

    override fun <T> session(block: QueryContext.() -> T): T = block(ctx)

    override fun <T> transaction(block: QueryContext.() -> T): T = block(ctx)
}
