package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object DeltakerFixture {
    val Deltaker = DeltakerDbo(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
        status = Deltakerstatus.DELTAR,
        opphav = Deltakeropphav.AMT,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2050, 1, 1),
        registrertDato = LocalDateTime.of(2022, 12, 24, 12, 0),
    )
}
