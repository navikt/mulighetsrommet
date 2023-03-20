package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import java.time.LocalDate
import java.util.*

object TiltaksgjennomforingFixtures {
    val Oppfolging1 = TiltaksgjennomforingDbo(
        id = UUID.fromString("20ecff07-18e4-4dba-ada3-0ee7cab5892e"),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#123456",
        virksomhetsnummer = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        enhet = "2990",
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        avtaleId = 1000
    )

    val Oppfolging2 = TiltaksgjennomforingDbo(
        id = UUID.fromString("170e298a-3431-4959-94ba-d717e640f4a5"),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#123123",
        virksomhetsnummer = null,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        enhet = "2990",
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        avtaleId = 1000
    )
}
