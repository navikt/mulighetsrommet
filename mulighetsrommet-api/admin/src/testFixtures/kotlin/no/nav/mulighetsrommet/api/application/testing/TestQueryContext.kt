package no.nav.mulighetsrommet.api.application.testing

import io.mockk.mockk
import no.nav.mulighetsrommet.api.application.Outbox
import no.nav.mulighetsrommet.api.application.QueryContext
import no.nav.mulighetsrommet.api.application.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository

class TestQueryContext : QueryContext() {
    var redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository = FakeRedaksjoneltInnholdLenkeRepository()
    var tiltakstypeRepository: TiltakstypeRepository = mockk(relaxed = true)
    var tiltakstypeQueries: TiltakstypeQueryHandler = mockk(relaxed = true)
    var endringshistorikk: EndringshistorikkQueryHandler = mockk(relaxed = true)

    override val repository = object : Repositories() {
        override val tiltakstype get() = tiltakstypeRepository
        override val redaksjoneltInnholdLenke get() = this@TestQueryContext.redaksjoneltInnholdLenke
    }

    override val queries = object : Queries() {
        override val tiltakstype get() = tiltakstypeQueries
        override val endringshistorikk get() = this@TestQueryContext.endringshistorikk
    }

    override val outbox: Outbox = mockk(relaxed = true)
}
