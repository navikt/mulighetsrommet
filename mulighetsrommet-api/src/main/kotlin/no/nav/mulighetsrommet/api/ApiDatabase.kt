package no.nav.mulighetsrommet.api

import kotliquery.Session
import no.nav.mulighetsrommet.altinn.db.AltinnRettigheterQueries
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorQueries
import no.nav.mulighetsrommet.api.avtale.db.AvtaleQueries
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggQueries
import no.nav.mulighetsrommet.api.datavarehus.db.DatavarehusTiltakQueries
import no.nav.mulighetsrommet.api.endringshistorikk.EndringshistorikkQueries
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingQueries
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattQueries
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslagQueries
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerQueries
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingQueries
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingQueries
import no.nav.mulighetsrommet.api.veilederflate.VeilederJoyrideQueries
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakQueries
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.notifications.NotificationQueries
import no.nav.mulighetsrommet.utdanning.db.UtdanningQueries
import javax.sql.DataSource

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

class QueryContext(val session: Session) {
    val queries by lazy { Queries() }

    inner class Queries {
        val enhet = NavEnhetQueries(session)
        val ansatt = NavAnsattQueries(session)
        val arrangor = ArrangorQueries(session)
        val tiltakstype = TiltakstypeQueries(session)
        val avtale = AvtaleQueries(session)
        val opsjoner = OpsjonLoggQueries(session)
        val gjennomforing = GjennomforingQueries(session)
        val deltaker = DeltakerQueries(session)
        val deltakerForslag = DeltakerForslagQueries(session)
        val utbetaling = UtbetalingQueries(session)
        val utdanning = UtdanningQueries(session)
        val dvh = DatavarehusTiltakQueries(session)
        val altinnRettigheter = AltinnRettigheterQueries(session)
        val tilsagn = TilsagnQueries(session)
        val notifications = NotificationQueries(session)
        val endringshistorikk = EndringshistorikkQueries(session)
        val delutbetaling = DelutbetalingQueries(session)
        val totrinnskontroll = TotrinnskontrollQueries(session)

        val veilderTiltak = VeilederflateTiltakQueries(session)
        val veilederJoyride = VeilederJoyrideQueries(session)
    }
}
