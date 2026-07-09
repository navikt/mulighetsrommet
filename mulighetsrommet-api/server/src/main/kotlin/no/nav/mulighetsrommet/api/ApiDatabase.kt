package no.nav.mulighetsrommet.api

import kotliquery.Session
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.altinn.db.AltinnRettigheterQueries
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorQueries
import no.nav.mulighetsrommet.api.arrangorflate.db.ArrangorflateQueries
import no.nav.mulighetsrommet.api.avtale.db.AvtaleQueries
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggQueries
import no.nav.mulighetsrommet.api.avtale.db.PrismodellQueries
import no.nav.mulighetsrommet.api.avtale.db.RammedetaljerQueries
import no.nav.mulighetsrommet.api.brukerutbetaling.db.BrukerUtbetalingQueries
import no.nav.mulighetsrommet.api.datavarehus.db.DatavarehusTiltakQueries
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingQueries
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattQueries
import no.nav.mulighetsrommet.api.persistence.endringshistorikk.EndringshistorikkQueries
import no.nav.mulighetsrommet.api.persistence.kostnadssted.db.KostnadsstedQueries
import no.nav.mulighetsrommet.api.persistence.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.api.persistence.redaksjoneltinnhold.RedaksjoneltInnholdLenkeQueries
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnQueries
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.OpplaeringtilskuddQueries
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingQueries
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslagQueries
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerQueries
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingLinjeQueries
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingQueries
import no.nav.mulighetsrommet.api.veilederflate.db.VeilederflateTiltakQueries
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.queries.KafkaConsumerRecordQueries
import no.nav.mulighetsrommet.database.queries.ScheduledTaskQueries
import no.nav.mulighetsrommet.kafka.KafkaProducerRecordQueries
import no.nav.mulighetsrommet.notifications.NotificationQueries
import no.nav.mulighetsrommet.oppgaver.OppgaveQueries
import no.nav.mulighetsrommet.utdanning.db.UtdanningQueries
import javax.sql.DataSource

class ApiDatabase(
    @PublishedApi
    internal val db: Database,
    @PublishedApi
    internal val topics: KafkaTopics,
) {

    fun getDatasource(): DataSource = db.getDatasource()

    inline fun <T> session(
        operation: QueryContext.() -> T,
    ): T {
        return db.session { session ->
            QueryContext(session, topics).operation()
        }
    }

    inline fun <T> transaction(
        operation: TransactionalQueryContext.() -> T,
    ): T {
        return db.transaction { session ->
            TransactionalQueryContext(session, topics).operation()
        }
    }
}

open class QueryContext(open val session: Session, topics: KafkaTopics) {
    val queries by lazy { Queries() }

    val outbox by lazy { OutboxEventPublisher(session, topics) }

    inner class Queries {
        val enhet = NavEnhetQueries(session)
        val kostnadssted = KostnadsstedQueries(session)
        val ansatt = NavAnsattQueries(session)
        val arrangor = ArrangorQueries(session)
        val tiltakstype = TiltakstypeQueries(session)
        val regelverklenke = RedaksjoneltInnholdLenkeQueries(session)
        val avtale = AvtaleQueries(session)
        val prismodell = PrismodellQueries(session)
        val rammedetaljer = RammedetaljerQueries(session)
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
        val utbetalingLinje = UtbetalingLinjeQueries(session)
        val totrinnskontroll = TotrinnskontrollQueries(session)
        val veilderTiltak = VeilederflateTiltakQueries(session)
        val kafkaProducerRecord = KafkaProducerRecordQueries(session)
        val oppgave = OppgaveQueries(session)
        val arrangorflate = ArrangorflateQueries(session)
        val scheduledTask = ScheduledTaskQueries(session)
        val kafkaConsumerRecords = KafkaConsumerRecordQueries(session)
        val opplaeringtilskudd = OpplaeringtilskuddQueries(session)
        val tilskuddBehandling = TilskuddBehandlingQueries(session)
        val brukerUtbetaling = BrukerUtbetalingQueries(session)
    }

    val repository by lazy { Repositories() }

    inner class Repositories {
        val tiltakstype: TiltakstypeRepository = queries.tiltakstype
    }
}

class TransactionalQueryContext(
    override val session: TransactionalSession,
    topics: KafkaTopics,
) : QueryContext(session, topics)
