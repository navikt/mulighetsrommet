package no.nav.mulighetsrommet.admin

import no.nav.mulighetsrommet.admin.arrangor.ArrangorQueryHandler
import no.nav.mulighetsrommet.admin.avtale.AvtaleQueryHandler
import no.nav.mulighetsrommet.admin.endringshistorikk.EndringshistorikkQueryHandler
import no.nav.mulighetsrommet.admin.gjennomforing.EnkeltplassPrisendringHandler
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingQueryHandler
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingWriter
import no.nav.mulighetsrommet.admin.kostnadssted.KostnadsstedQueryHandler
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDtoQueryHandler
import no.nav.mulighetsrommet.admin.notifications.NotificationWriter
import no.nav.mulighetsrommet.admin.opplaring.OpplaringKategoriseringQueryHandler
import no.nav.mulighetsrommet.admin.tiltak.AdminTiltakstypeQueryHandler
import no.nav.mulighetsrommet.admin.tiltakbeskrivelse.TiltakBeskrivelseQueryHandler
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollQueryHandler
import no.nav.mulighetsrommet.api.contracts.gjennomforing.TiltaksgjennomforingV2Dto
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorRepository
import no.nav.mulighetsrommet.api.domain.avtale.AvtaleRepository
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslagRepository
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerRepository
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingRepository
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRepository
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetRepository
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenkeRepository
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellRepository
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeRepository
import no.nav.mulighetsrommet.api.domain.tiltakbeskrivelse.TiltakBeskrivelseRepository
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramRepository
import java.util.UUID

abstract class QueryContext {
    abstract val repository: Repositories
    abstract val queries: Queries
    abstract val outbox: Outbox
    abstract val commands: Commands

    abstract class Repositories {
        abstract val tiltakstype: TiltakstypeRepository
        abstract val avtale: AvtaleRepository
        abstract val redaksjoneltInnholdLenke: RedaksjoneltInnholdLenkeRepository
        abstract val navEnhet: NavEnhetRepository
        abstract val navAnsatt: NavAnsattRepository
        abstract val arrangor: ArrangorRepository
        abstract val utdanning: UtdanningsprogramRepository
        abstract val tiltakBeskrivelse: TiltakBeskrivelseRepository
        abstract val deltaker: DeltakerRepository
        abstract val deltakerForslag: DeltakerForslagRepository
        abstract val gjennomforing: GjennomforingRepository
        abstract val prismodell: PrismodellRepository
    }

    abstract class Queries {
        abstract val tiltakstype: AdminTiltakstypeQueryHandler
        abstract val avtale: AvtaleQueryHandler
        abstract val endringshistorikk: EndringshistorikkQueryHandler
        abstract val kostnadssted: KostnadsstedQueryHandler
        abstract val navAnsattDto: NavAnsattDtoQueryHandler
        abstract val totrinnskontroll: TotrinnskontrollQueryHandler
        abstract val arrangor: ArrangorQueryHandler
        abstract val opplaering: OpplaringKategoriseringQueryHandler
        abstract val tiltakBeskrivelse: TiltakBeskrivelseQueryHandler
        abstract val gjennomforing: GjennomforingQueryHandler
    }

    abstract class Commands {
        abstract val gjennomforing: GjennomforingWriter
        abstract val enkeltplassPrisendring: EnkeltplassPrisendringHandler
        abstract val notifications: NotificationWriter
    }

    interface Outbox {
        fun publish(tiltakstype: Tiltakstype)

        fun publish(totrinnskontroll: Totrinnskontroll)

        fun publish(id: UUID, dto: TiltaksgjennomforingV2Dto?)
    }
}
