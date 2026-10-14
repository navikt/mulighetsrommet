package no.nav.mulighetsrommet.api.persistence.veilederflate

import kotliquery.Session
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerQueryHandler
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.persistence.tiltak.AdminTiltakstypeQueries
import no.nav.mulighetsrommet.api.veilederflate.QueryContext
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakQueryHandler
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakstypeQueryHandler

/**
 * Concrete [QueryContext] backed by JDBC/kotliquery.
 */
class SqlVeilederflateQueryContext(session: Session) : QueryContext() {
    val veilderTiltak = VeilederflateTiltakQueries(session)
    val tiltakstype = AdminTiltakstypeQueries(session)
    val veilederflateTiltakstype = VeilederflateTiltakstypeQueries(session)
    val delMedBruker = DelMedBrukerQueries(session)

    override val queries = object : Queries() {
        override val veilederTiltak: VeilederflateTiltakQueryHandler = this@SqlVeilederflateQueryContext.veilderTiltak
        override val tiltakstype: VeilederflateTiltakstypeQueryHandler = this@SqlVeilederflateQueryContext.veilederflateTiltakstype
        override val delMedBruker: DelMedBrukerQueryHandler = this@SqlVeilederflateQueryContext.delMedBruker
    }

    override val repository = object : Repositories() {
        override val tiltakstype: TiltakstypeRepository = this@SqlVeilederflateQueryContext.tiltakstype
    }
}
