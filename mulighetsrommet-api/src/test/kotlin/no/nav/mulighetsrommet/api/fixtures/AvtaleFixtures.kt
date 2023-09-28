package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.routes.v1.AvtaleRequest
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.VirksomhetKontaktperson
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val avtale1 = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = emptyList(),
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.of(2023, 2, 28),
        navRegion = "2990",
        avtaletype = Avtaletype.Rammeavtale,
        prisbetingelser = null,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        administratorer = emptyList(),
        navEnheter = emptyList(),
        leverandorKontaktpersonId = null,
        antallPlasser = null,
        url = null,
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
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        administrator = "DD1",
        url = "google.com",
        navEnheter = listOf("2990"),
        leverandorKontaktpersonId = null,
        prisOgBetalingsinformasjon = null,
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

    val avtaleAdminDto = AvtaleAdminDto(
        id = avtale1.id,
        tiltakstype = AvtaleAdminDto.Tiltakstype(
            id = TiltakstypeFixtures.AFT.id,
            navn = TiltakstypeFixtures.AFT.navn,
            arenaKode = TiltakstypeFixtures.AFT.tiltakskode,
        ),
        navn = avtale1.navn,
        avtalenummer = avtale1.navn,
        leverandor = AvtaleAdminDto.Leverandor(organisasjonsnummer = "123456789", navn = "Bedrift", slettet = false),
        leverandorUnderenheter = emptyList(),
        leverandorKontaktperson = VirksomhetKontaktperson(
            id = UUID.randomUUID(),
            organisasjonsnummer = "123456789",
            navn = "Ole",
            beskrivelse = null,
            telefon = null,
            epost = "ole@bedrift.no",
        ),
        startDato = avtale1.startDato,
        sluttDato = avtale1.sluttDato,
        navRegion = null,
        avtaletype = Avtaletype.Avtale,
        avtalestatus = Avtalestatus.Aktiv,
        prisbetingelser = null,
        administrator = null,
        url = null,
        antallPlasser = null,
        navEnheter = emptyList(),
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
    )
}
