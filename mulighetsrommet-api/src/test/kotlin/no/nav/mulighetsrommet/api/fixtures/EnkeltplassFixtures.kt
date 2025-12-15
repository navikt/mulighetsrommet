package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingEnkeltplassDbo
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.time.LocalDate
import java.util.UUID

object EnkeltplassFixtures {
    val EnkelAmo = GjennomforingEnkeltplassDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Enkelamo 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 1,
    )

    val EnkelAmo2 = GjennomforingEnkeltplassDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Enkelamo 2",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 1,
    )
}
