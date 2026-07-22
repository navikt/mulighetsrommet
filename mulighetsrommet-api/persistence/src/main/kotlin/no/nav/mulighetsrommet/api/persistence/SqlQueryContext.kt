package no.nav.mulighetsrommet.api.persistence

import kotliquery.Session
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.arrangor.ArrangorQueryHandler
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.admin.kostnadssted.KostnadsstedQueryHandler
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQueryHandler
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentQueryHandler
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollQueryHandler
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorRepository
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerRepository
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokumentRepository
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramRepository
import no.nav.mulighetsrommet.api.persistence.arrangor.ArrangorQueries
import no.nav.mulighetsrommet.api.persistence.deltaker.DeltakerForslagQueries
import no.nav.mulighetsrommet.api.persistence.deltaker.DeltakerQueries
import no.nav.mulighetsrommet.api.persistence.endringshistorikk.EndringshistorikkQueries
import no.nav.mulighetsrommet.api.persistence.kostnadssted.KostnadsstedQueries
import no.nav.mulighetsrommet.api.persistence.navansatt.NavAnsattDtoQueries
import no.nav.mulighetsrommet.api.persistence.navansatt.NavAnsattQueries
import no.nav.mulighetsrommet.api.persistence.navenhet.NavEnhetQueries
import no.nav.mulighetsrommet.api.persistence.opplaring.OpplaringKategoriseringQueries
import no.nav.mulighetsrommet.api.persistence.redaksjoneltinnhold.RedaksjoneltInnholdLenkeQueries
import no.nav.mulighetsrommet.api.persistence.tiltak.PrismodellQueries
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries
import no.nav.mulighetsrommet.api.persistence.tiltakdokument.TiltakDokumentQueries
import no.nav.mulighetsrommet.api.persistence.totrinnskontroll.TotrinnskontrollQueries
import no.nav.mulighetsrommet.api.persistence.utdanning.UtdanningQueries

/**
 * Concrete [QueryContext] backed by JDBC/kotliquery.
 */
class SqlQueryContext(session: Session, topics: OutboxTopics) : QueryContext() {
    val tiltakstype = TiltakstypeQueries(session)
    val prismodell = PrismodellQueries(session)
    val endringshistorikk = EndringshistorikkQueries(session)
    val redaksjoneltInnholdLenke = RedaksjoneltInnholdLenkeQueries(session)
    val navEnhet = NavEnhetQueries(session)
    val navAnsatt = NavAnsattQueries(session)
    val kostnadssted = KostnadsstedQueries(session)
    val navAnsattDto = NavAnsattDtoQueries(session)
    val totrinnskontroll = TotrinnskontrollQueries(session)
    val arrangor = ArrangorQueries(session)
    val opplaering = OpplaringKategoriseringQueries(session)
    val utdanning = UtdanningQueries(session)
    val tiltakDokument = TiltakDokumentQueries(session)
    val deltaker = DeltakerQueries(session)
    val deltakerForslag = DeltakerForslagQueries(session)

    override val repository = object : Repositories() {
        override val tiltakstype: TiltakstypeRepository = this@SqlQueryContext.tiltakstype
        override val redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository = this@SqlQueryContext.redaksjoneltInnholdLenke
        override val navEnhet: NavEnhetRepository = this@SqlQueryContext.navEnhet
        override val navAnsatt: NavAnsattRepository = this@SqlQueryContext.navAnsatt
        override val arrangor: ArrangorRepository = this@SqlQueryContext.arrangor
        override val utdanning: UtdanningsprogramRepository = this@SqlQueryContext.utdanning
        override val tiltakDokument: TiltakDokumentRepository = this@SqlQueryContext.tiltakDokument
        override val deltaker: DeltakerRepository = this@SqlQueryContext.deltaker
        override val deltakerForslag: DeltakerForslagRepository = this@SqlQueryContext.deltakerForslag
    }

    override val queries = object : Queries() {
        override val tiltakstype: TiltakstypeQueryHandler = this@SqlQueryContext.tiltakstype
        override val endringshistorikk: EndringshistorikkQueryHandler = this@SqlQueryContext.endringshistorikk
        override val kostnadssted: KostnadsstedQueryHandler = this@SqlQueryContext.kostnadssted
        override val navAnsattDto: NavAnsattDtoQueryHandler = this@SqlQueryContext.navAnsattDto
        override val totrinnskontroll: TotrinnskontrollQueryHandler = this@SqlQueryContext.totrinnskontroll
        override val arrangor: ArrangorQueryHandler = this@SqlQueryContext.arrangor
        override val opplaering: OpplaringKategoriseringQueryHandler = this@SqlQueryContext.opplaering
        override val tiltakDokument: TiltakDokumentQueryHandler = this@SqlQueryContext.tiltakDokument
    }

    override val outbox = SqlAdminOutbox(session, topics)
}
