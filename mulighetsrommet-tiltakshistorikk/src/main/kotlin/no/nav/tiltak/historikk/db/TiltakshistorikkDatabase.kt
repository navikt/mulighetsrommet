package no.nav.tiltak.historikk.db

import kotliquery.Session
import no.nav.mulighetsrommet.database.Database
import no.nav.tiltak.historikk.db.queries.ArenaDeltakerQueries
import no.nav.tiltak.historikk.db.queries.GjennomforingQueries
import no.nav.tiltak.historikk.db.queries.GruppetiltakQueries
import no.nav.tiltak.historikk.db.queries.KometDeltakerQueries
import no.nav.tiltak.historikk.db.queries.VirksomhetQueries

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
        val gjennomforing = GjennomforingQueries(session)
        val kometDeltaker = KometDeltakerQueries(session)
        val arenaDeltaker = ArenaDeltakerQueries(session)
        val virksomhet = VirksomhetQueries(session)
    }
}
