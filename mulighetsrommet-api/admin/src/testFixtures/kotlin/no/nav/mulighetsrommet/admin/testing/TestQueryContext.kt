package no.nav.mulighetsrommet.admin.testing

import io.mockk.mockk
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.arrangor.ArrangorQueryHandler
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.admin.kostnadssted.KostnadsstedQueryHandler
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQueryHandler
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentQueryHandler
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollQueryHandler

class TestQueryContext : QueryContext() {
    private val redaksjoneltInnholdLenkeRepository = FakeRedaksjoneltInnholdLenkeRepository()
    private val tiltakstypeRepository = FakeTiltakstypeRepository()
    private val navEnhetRepository = FakeNavEnhetRepository()
    private val navAnsattRepository = FakeNavAnsattRepository()
    private val arrangorRepository = FakeArrangorRepository()
    private val utdanningRepository = FakeUtdanningRepository()
    private val tiltakDokumentRepository = FakeTiltakDokumentRepository()

    private var tiltakstype: TiltakstypeQueryHandler = mockk(relaxed = true)
    private var endringshistorikk: EndringshistorikkQueryHandler = mockk(relaxed = true)
    private var kostnadssted: KostnadsstedQueryHandler = mockk(relaxed = true)
    private var navAnsattDto: NavAnsattDtoQueryHandler = mockk(relaxed = true)
    private var totrinnskontroll: TotrinnskontrollQueryHandler = mockk(relaxed = true)
    private var arrangor: ArrangorQueryHandler = mockk(relaxed = true)
    private var opplaringKategorisering: OpplaringKategoriseringQueryHandler = mockk(relaxed = true)
    private var tiltakDokument: TiltakDokumentQueryHandler = mockk(relaxed = true)

    override val repository = object : Repositories() {
        override val tiltakstype get() = tiltakstypeRepository
        override val redaksjoneltInnholdLenke get() = redaksjoneltInnholdLenkeRepository
        override val navEnhet get() = navEnhetRepository
        override val navAnsatt get() = navAnsattRepository
        override val arrangor get() = arrangorRepository
        override val utdanning get() = utdanningRepository
        override val tiltakDokument get() = tiltakDokumentRepository
    }

    override val queries = object : Queries() {
        override val tiltakstype get() = this@TestQueryContext.tiltakstype
        override val endringshistorikk get() = this@TestQueryContext.endringshistorikk
        override val kostnadssted get() = this@TestQueryContext.kostnadssted
        override val navAnsattDto get() = this@TestQueryContext.navAnsattDto
        override val totrinnskontroll get() = this@TestQueryContext.totrinnskontroll
        override val arrangor get() = this@TestQueryContext.arrangor
        override val opplaringKategorisering get() = this@TestQueryContext.opplaringKategorisering
        override val tiltakDokument get() = this@TestQueryContext.tiltakDokument
    }

    override val outbox: Outbox = mockk(relaxed = true)
}
