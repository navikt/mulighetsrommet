package no.nav.mulighetsrommet.api.veilederflate.testing

import io.mockk.mockk
import no.nav.mulighetsrommet.api.delmedbruker.DelMedBrukerQueryHandler
import no.nav.mulighetsrommet.api.domain.testing.repository.FakeTiltakstypeRepository
import no.nav.mulighetsrommet.api.veilederflate.QueryContext
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakQueryHandler
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakstypeQueryHandler

class TestQueryContext : QueryContext() {
    private val tiltakstypeRepository = FakeTiltakstypeRepository()

    private var veilederTiltak: VeilederflateTiltakQueryHandler = mockk(relaxed = true)
    private var tiltakstype: VeilederflateTiltakstypeQueryHandler = mockk(relaxed = true)
    private var delMedBruker: DelMedBrukerQueryHandler = mockk(relaxed = true)

    override val repository = object : Repositories() {
        override val tiltakstype get() = tiltakstypeRepository
    }

    override val queries = object : Queries() {
        override val veilederTiltak get() = this@TestQueryContext.veilederTiltak
        override val tiltakstype get() = this@TestQueryContext.tiltakstype
        override val delMedBruker get() = this@TestQueryContext.delMedBruker
    }
}
