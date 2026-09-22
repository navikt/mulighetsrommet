package no.nav.mulighetsrommet.api.persistence.veilederflate

import no.nav.mulighetsrommet.api.veilederflate.QueryContext
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateDatabase
import no.nav.mulighetsrommet.database.Database

class SqlVeilederflateDatabase(
    private val db: Database,
) : VeilederflateDatabase {
    override fun <T> session(block: QueryContext.() -> T): T = db.session { session -> SqlVeilederflateQueryContext(session).block() }

    override fun <T> transaction(block: QueryContext.() -> T): T = db.transaction { session -> SqlVeilederflateQueryContext(session).block() }
}
