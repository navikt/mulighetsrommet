package no.nav.mulighetsrommet.api.application.testing

import io.mockk.mockk
import no.nav.mulighetsrommet.api.application.Outbox
import no.nav.mulighetsrommet.api.application.QueryContext
import no.nav.mulighetsrommet.api.application.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.api.application.tiltak.TiltakstypeQueryHandler

class TestQueryContext : QueryContext() {
    private val redaksjoneltInnholdLenkeRepository = FakeRedaksjoneltInnholdLenkeRepository()
    private val tiltakstypeRepository = FakeTiltakstypeRepository()

    private var tiltakstype: TiltakstypeQueryHandler = mockk(relaxed = true)
    private var endringshistorikk: EndringshistorikkQueryHandler = mockk(relaxed = true)

    override val repository = object : Repositories() {
        override val tiltakstype get() = tiltakstypeRepository
        override val redaksjoneltInnholdLenke get() = redaksjoneltInnholdLenkeRepository
    }

    override val queries = object : Queries() {
        override val tiltakstype get() = this@TestQueryContext.tiltakstype
        override val endringshistorikk get() = this@TestQueryContext.endringshistorikk
    }

    override val outbox: Outbox = mockk(relaxed = true)
}
