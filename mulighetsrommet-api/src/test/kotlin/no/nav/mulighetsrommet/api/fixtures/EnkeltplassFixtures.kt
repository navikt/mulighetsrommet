package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.gjennomforing.db.EnkeltplassDbo
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.util.*

object EnkeltplassFixtures {
    val EnkelAmo = EnkeltplassDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = AvtaleFixtures.oppfolging.startDato,
        sluttDato = AvtaleFixtures.oppfolging.sluttDato,
        status = GjennomforingStatusType.GJENNOMFORES,
        amoKategorisering = null,
        utdanningslop = null,
    )
}
