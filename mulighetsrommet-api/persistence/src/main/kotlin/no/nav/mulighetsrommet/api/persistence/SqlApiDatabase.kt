package no.nav.mulighetsrommet.api.persistence

import no.nav.mulighetsrommet.api.application.ApiDatabase
import no.nav.mulighetsrommet.api.application.QueryContext
import no.nav.mulighetsrommet.database.Database

class SqlApiDatabase(
    private val db: Database,
    private val topics: OutboxTopics,
) : ApiDatabase {
    override fun <T> session(block: QueryContext.() -> T): T = db.session { session ->
        SqlQueryContext(session, topics).block()
    }

    override fun <T> transaction(block: QueryContext.() -> T): T = db.transaction { session ->
        SqlQueryContext(session, topics).block()
    }
}
