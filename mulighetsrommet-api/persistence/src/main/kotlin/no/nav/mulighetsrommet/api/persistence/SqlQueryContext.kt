package no.nav.mulighetsrommet.api.persistence

import kotliquery.Session
import no.nav.mulighetsrommet.api.application.QueryContext
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries

class SqlQueryContext(session: Session, topics: OutboxTopics) : QueryContext() {
    private val tiltakstypeDao = TiltakstypeQueries(session)

    override val repository = object : Repositories() {
        override val tiltakstype: TiltakstypeRepository = tiltakstypeDao
    }

    override val queries = object : Queries() {
        override val tiltakstype: TiltakstypeQueryHandler = tiltakstypeDao
    }

    override val outbox = SqlOutbox(session, topics)
}
