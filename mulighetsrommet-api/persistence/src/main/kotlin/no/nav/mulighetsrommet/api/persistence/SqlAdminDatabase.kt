package no.nav.mulighetsrommet.api.persistence

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.database.Database

class SqlAdminDatabase(
    private val db: Database,
    private val topics: OutboxTopics,
) : AdminDatabase {
    override fun <T> session(block: QueryContext.() -> T): T = db.session { session -> SqlQueryContext(session, topics).block() }

    override fun <T> transaction(block: QueryContext.() -> T): T = db.transaction { session -> SqlQueryContext(session, topics).block() }
}
