package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo.Oppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingDbo.Tilgjengelighetsstatus
import java.time.LocalDate
import java.util.*

object TiltaksgjennomforingFixtures {
    val Oppfolging1 = TiltaksgjennomforingDbo(
        id = UUID.fromString("20ecff07-18e4-4dba-ada3-0ee7cab5892e"),
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#1",
        virksomhetsnummer = "976663934",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = Tilgjengelighetsstatus.LEDIG,
        antallPlasser = null,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = Oppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
    )

    val Oppfolging2 = TiltaksgjennomforingDbo(
        id = UUID.fromString("170e298a-3431-4959-94ba-d717e640f4a5"),
        navn = "Oppfølging 2",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        tiltaksnummer = "2023#2",
        virksomhetsnummer = "111111111",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = Tilgjengelighetsstatus.LEDIG,
        antallPlasser = null,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = Oppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
    )

    val Arbeidstrening1 = TiltaksgjennomforingDbo(
        id = UUID.fromString("baae02dc-28c8-4382-be66-6b185adcdd08"),
        navn = "Arbeidstrening 1",
        tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
        tiltaksnummer = "2023#3",
        virksomhetsnummer = "222222222",
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        tilgjengelighet = Tilgjengelighetsstatus.LEDIG,
        antallPlasser = null,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = Oppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        kontaktpersoner = emptyList(),
    )
}
