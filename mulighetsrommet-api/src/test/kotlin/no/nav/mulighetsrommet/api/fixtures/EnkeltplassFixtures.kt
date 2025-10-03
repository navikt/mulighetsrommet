package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.gjennomforing.db.EnkeltplassDbo
import java.util.*

object EnkeltplassFixtures {
    val EnkelAmo = EnkeltplassDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
    )
}
