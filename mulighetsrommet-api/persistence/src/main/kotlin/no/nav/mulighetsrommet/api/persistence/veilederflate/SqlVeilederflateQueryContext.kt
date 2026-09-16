package no.nav.mulighetsrommet.api.persistence.veilederflate

import kotliquery.Session
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerQueryHandler
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.persistence.tiltak.TiltakstypeQueries
import no.nav.mulighetsrommet.api.veilederflate.QueryContext
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakQueryHandler

/**
 * Concrete [QueryContext] backed by JDBC/kotliquery.
 */
class SqlVeilederflateQueryContext(session: Session) : QueryContext() {
    val veilderTiltak = VeilederflateTiltakQueries(session)
    val tiltakstype = TiltakstypeQueries(session)
    val delMedBruker = DelMedBrukerQueries(session)

    override val queries = object : Queries() {
        override val veilederTiltak: VeilederflateTiltakQueryHandler = this@SqlVeilederflateQueryContext.veilderTiltak
        override val tiltakstype: TiltakstypeQueryHandler = this@SqlVeilederflateQueryContext.tiltakstype
        override val delMedBruker: DelMedBrukerQueryHandler = this@SqlVeilederflateQueryContext.delMedBruker
    }

    override val repository = object : Repositories() {
        override val tiltakstype: TiltakstypeRepository = this@SqlVeilederflateQueryContext.tiltakstype
    }
}
