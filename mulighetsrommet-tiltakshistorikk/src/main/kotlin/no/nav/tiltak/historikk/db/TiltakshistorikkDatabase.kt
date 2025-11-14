package no.nav.tiltak.historikk.db

import kotliquery.Session
import no.nav.mulighetsrommet.database.Database

class TiltakshistorikkDatabase(
    @PublishedApi
    internal val db: Database,
) {

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
        val gruppetiltak = GruppetiltakQueries(session)
        val kometDeltaker = KometDeltakerQueries(session)
        val arenaDeltaker = ArenaDeltakerQueries(session)
    }
}
