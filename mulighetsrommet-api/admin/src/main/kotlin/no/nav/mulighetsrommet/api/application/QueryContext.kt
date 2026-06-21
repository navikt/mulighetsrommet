package no.nav.mulighetsrommet.api.application

import no.nav.mulighetsrommet.api.application.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository

abstract class QueryContext {
    abstract val repository: Repositories
    abstract val queries: Queries
    abstract val outbox: Outbox

    abstract class Repositories {
        abstract val tiltakstype: TiltakstypeRepository
        abstract val redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository
    }

    abstract class Queries {
        abstract val tiltakstype: TiltakstypeQueryHandler
        abstract val endringshistorikk: EndringshistorikkQueryHandler
    }
}
