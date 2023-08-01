package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
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
        navRegion = null,
        avtaletype = Avtaletype.Rammeavtale,
        avslutningsstatus = Avslutningsstatus.IKKE_AVSLUTTET,
        prisbetingelser = null,
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        leverandorKontaktpersonId = null,
    )
}
