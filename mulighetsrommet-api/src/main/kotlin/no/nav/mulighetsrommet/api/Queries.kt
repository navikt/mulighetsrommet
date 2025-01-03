package no.nav.mulighetsrommet.api

import no.nav.mulighetsrommet.api.arrangor.db.ArrangorQueries
import no.nav.mulighetsrommet.api.avtale.db.AvtaleQueries
import no.nav.mulighetsrommet.api.avtale.db.OpsjonLoggQueries
import no.nav.mulighetsrommet.api.datavarehus.db.DatavarehusTiltakQueries
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingQueries
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattQueries
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetQueries
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerForslagQueries
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerQueries
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravQueries
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeQueries
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakQueries
import no.nav.mulighetsrommet.utdanning.db.UtdanningQueries

object Queries {
    val enhet = NavEnhetQueries
    val ansatt = NavAnsattQueries
    val arrangor = ArrangorQueries
    val tiltakstype = TiltakstypeQueries
    val avtale = AvtaleQueries
    val opsjoner = OpsjonLoggQueries
    val gjennomforing = TiltaksgjennomforingQueries
    val deltaker = DeltakerQueries
    val deltakerForslag = DeltakerForslagQueries
    val refusjonskrav = RefusjonskravQueries
    val utdanning = UtdanningQueries

    val dvh = DatavarehusTiltakQueries

    val veilderTiltak = VeilederflateTiltakQueries
}
