package no.nav.mulighetsrommet.api

import kotliquery.Session
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.altinn.db.AltinnRettigheterQueries
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorQueries
import no.nav.mulighetsrommet.api.avtale.db.AvtaleQueries
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggQueries
import no.nav.mulighetsrommet.api.datavarehus.db.DatavarehusTiltakQueries
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkQueries
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingQueries
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattQueries
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagQueries
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerQueries
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravQueries
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeQueries
import no.nav.mulighetsrommet.api.veilederflate.VeilederJoyrideQueries
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakQueries
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.notifications.NotificationQueries
import no.nav.mulighetsrommet.utdanning.db.UtdanningQueries
import javax.sql.DataSource

/**
 * Kjører [block] i kontekst av en [TransactionalSession], utledet fra [session] (som allerede kan være en [Session]
 * eller en [TransactionalSession]).
 */
inline fun <R> withTransaction(session: Session, block: TransactionalSession.() -> R): R {
    return if (session is TransactionalSession) {
        session.block()
    } else {
        session.transaction { it.block() }
    }
}

class QueryContext(val session: Session) {
    val queries by lazy { Queries() }

    inner class Queries {
        val enhet = NavEnhetQueries(session)
        val ansatt = NavAnsattQueries(session)
        val arrangor = ArrangorQueries(session)
        val tiltakstype = TiltakstypeQueries(session)
        val avtale = AvtaleQueries(session)
        val opsjoner = OpsjonLoggQueries(session)
        val gjennomforing = TiltaksgjennomforingQueries(session)
        val deltaker = DeltakerQueries(session)
        val deltakerForslag = DeltakerForslagQueries(session)
        val refusjonskrav = RefusjonskravQueries(session)
        val utdanning = UtdanningQueries(session)
        val dvh = DatavarehusTiltakQueries(session)
        val altinnRettigheter = AltinnRettigheterQueries(session)
        val tilsagn = TilsagnQueries(session)
        val notifications = NotificationQueries(session)
        val endringshistorikk = EndringshistorikkQueries(session)

        val veilderTiltak = VeilederflateTiltakQueries(session)
        val veilederJoyride = VeilederJoyrideQueries(session)
    }
}

class ApiDatabase(
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
