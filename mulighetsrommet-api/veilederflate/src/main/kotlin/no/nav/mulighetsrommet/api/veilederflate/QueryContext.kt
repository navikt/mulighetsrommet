package no.nav.mulighetsrommet.api.veilederflate

import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerQueryHandler
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository

abstract class QueryContext {
    abstract val queries: Queries
    abstract val repository: Repositories

    abstract class Queries {
        abstract val veilederTiltak: VeilederflateTiltakQueryHandler
        abstract val tiltakstype: TiltakstypeQueryHandler
        abstract val delMedBruker: DelMedBrukerQueryHandler
    }

    abstract class Repositories {
        abstract val tiltakstype: TiltakstypeRepository
    }
}
