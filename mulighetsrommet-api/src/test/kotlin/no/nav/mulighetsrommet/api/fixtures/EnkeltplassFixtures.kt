package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.time.LocalDate
import java.util.UUID

object EnkeltplassFixtures {
    val EnkelAmo = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        type = GjennomforingType.ARENA_ENKELTPLASS,
        navn = "Enkelamo 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 1,
    )

    val EnkelAmo2 = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        type = GjennomforingType.ARENA_ENKELTPLASS,
        navn = "Enkelamo 2",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 1,
    )
}
