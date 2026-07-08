package no.nav.mulighetsrommet.api.persistence

import kotliquery.Session
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.persistence.endringshistorikk.EndringshistorikkQueries
import no.nav.mulighetsrommet.api.persistence.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.api.persistence.redaksjoneltinnhold.RedaksjoneltInnholdLenkeQueries
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries

class SqlQueryContext(session: Session, topics: OutboxTopics) : QueryContext() {
    private val tiltakstypeDao = TiltakstypeQueries(session)
    private val endringshistorikkDao = EndringshistorikkQueries(session)
    private val redaksjoneltInnholdDao = RedaksjoneltInnholdLenkeQueries(session)
    private val navEnhetDao = NavEnhetQueries(session)

    override val repository = object : Repositories() {
        override val tiltakstype: TiltakstypeRepository = tiltakstypeDao
        override val redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository = redaksjoneltInnholdDao
        override val navEnhet: NavEnhetRepository = navEnhetDao
    }

    override val queries = object : Queries() {
        override val tiltakstype: TiltakstypeQueryHandler = tiltakstypeDao
        override val endringshistorikk: EndringshistorikkQueryHandler = endringshistorikkDao
    }

    override val outbox = SqlAdminOutbox(session, topics)
}
