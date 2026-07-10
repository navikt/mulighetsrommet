package no.nav.mulighetsrommet.api.persistence

import kotliquery.Session
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.admin.kostnadssted.KostnadsstedQueryHandler
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.persistence.endringshistorikk.EndringshistorikkQueries
import no.nav.mulighetsrommet.api.persistence.kostnadssted.db.KostnadsstedQueries
import no.nav.mulighetsrommet.api.persistence.navansatt.db.NavAnsattDtoQueries
import no.nav.mulighetsrommet.api.persistence.navansatt.db.NavAnsattQueries
import no.nav.mulighetsrommet.api.persistence.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.api.persistence.redaksjoneltinnhold.RedaksjoneltInnholdLenkeQueries
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries

class SqlQueryContext(session: Session, topics: OutboxTopics) : QueryContext() {
    private val tiltakstypeDao = TiltakstypeQueries(session)
    private val endringshistorikkDao = EndringshistorikkQueries(session)
    private val redaksjoneltInnholdDao = RedaksjoneltInnholdLenkeQueries(session)
    private val navEnhetDao = NavEnhetQueries(session)
    private val navAnsattDao = NavAnsattQueries(session)
    private val kostnadsstedDao = KostnadsstedQueries(session)
    private val navAnsattDtoDao = NavAnsattDtoQueries(session)

    override val repository = object : Repositories() {
        override val tiltakstype: TiltakstypeRepository = tiltakstypeDao
        override val redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository = redaksjoneltInnholdDao
        override val navEnhet: NavEnhetRepository = navEnhetDao
        override val navAnsatt: NavAnsattRepository = navAnsattDao
    }

    override val queries = object : Queries() {
        override val tiltakstype: TiltakstypeQueryHandler = tiltakstypeDao
        override val endringshistorikk: EndringshistorikkQueryHandler = endringshistorikkDao
        override val kostnadssted: KostnadsstedQueryHandler = kostnadsstedDao
        override val navAnsattDto: NavAnsattDtoQueryHandler = navAnsattDtoDao
    }

    override val outbox = SqlAdminOutbox(session, topics)
}
