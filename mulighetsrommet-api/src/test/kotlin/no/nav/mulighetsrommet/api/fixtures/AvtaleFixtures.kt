package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val avtale1Id = UUID.randomUUID()
    val avtale1 = AvtaleDbo(
        id = avtale1Id,
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = emptyList(),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.of(2023, 2, 28),
        arenaAnsvarligEnhet = null,
        navRegion = "2990",
        avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        prisbetingelser = null,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        leverandorKontaktpersonId = null,
    )

    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = listOf("123456789"),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.of(2023, 2, 28),
        navRegion = "2990",
        avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        ansvarlig = "DD1",
        url = "google.com",
        navEnheter = listOf("2990"),
        leverandorKontaktpersonId = null,
    )

    val arenaAvtale1 = ArenaAvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorOrganisasjonsnummer = "123456789",
        arenaAnsvarligEnhet = null,
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.of(2023, 2, 28),
        avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        prisbetingelser = null,
        opphav = ArenaMigrering.Opphav.ARENA,
    )
}
