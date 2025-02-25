package no.nav.tiltak.okonomi.db

import kotliquery.Session
import no.nav.mulighetsrommet.database.Database
import no.nav.tiltak.okonomi.db.queries.BestillingQueries
import no.nav.tiltak.okonomi.db.queries.FakturaQueries
import no.nav.tiltak.okonomi.db.queries.TiltakKonteringQueries
import javax.sql.DataSource

class OkonomiDatabase(
    @PublishedApi
    internal val db: Database,
) {

    fun getDatasource(): DataSource = db.getDatasource()

    inline fun <T> session(
        operation: QueryContext.() -> T,
    ): T {
        return db.session { session ->
            QueryContext(session).operation()
        }
    }

    inline fun <T> transaction(
        operation: QueryContext.() -> T,
    ): T {
        return db.transaction { session ->
            QueryContext(session).operation()
        }
    }
}

class QueryContext(val session: Session) {
    val queries by lazy { Queries() }

    inner class Queries {
        val bestilling = BestillingQueries(session)
        val faktura = FakturaQueries(session)
        val kontering = TiltakKonteringQueries(session)
    }
}
