package no.nav.mulighetsrommet.admin

import no.nav.mulighetsrommet.admin.arrangor.ArrangorQueryHandler
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.admin.kostnadssted.KostnadsstedQueryHandler
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQueryHandler
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeQueryHandler
import no.nav.mulighetsrommet.admin.tiltakdokument.TiltakDokumentQueryHandler
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollQueryHandler
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorRepository
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerRepository
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokumentRepository
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramRepository

abstract class QueryContext {
    abstract val repository: Repositories
    abstract val queries: Queries
    abstract val outbox: Outbox

    abstract class Repositories {
        abstract val tiltakstype: TiltakstypeRepository
        abstract val redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository
        abstract val navEnhet: NavEnhetRepository
        abstract val navAnsatt: NavAnsattRepository
        abstract val arrangor: ArrangorRepository
        abstract val utdanning: UtdanningsprogramRepository
        abstract val tiltakDokument: TiltakDokumentRepository
        abstract val deltaker: DeltakerRepository
        abstract val deltakerForslag: DeltakerForslagRepository
    }

    abstract class Queries {
        abstract val tiltakstype: TiltakstypeQueryHandler
        abstract val endringshistorikk: EndringshistorikkQueryHandler
        abstract val kostnadssted: KostnadsstedQueryHandler
        abstract val navAnsattDto: NavAnsattDtoQueryHandler
        abstract val totrinnskontroll: TotrinnskontrollQueryHandler
        abstract val arrangor: ArrangorQueryHandler
        abstract val opplaering: OpplaringKategoriseringQueryHandler
        abstract val tiltakDokument: TiltakDokumentQueryHandler
    }

    interface Outbox {
        fun publish(tiltakstype: Tiltakstype)

        fun publish(totrinnskontroll: Totrinnskontroll)
    }
}
