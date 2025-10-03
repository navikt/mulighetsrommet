package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.api.AvtaleDetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtalePersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtaleVeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.OpsjonsmodellDbo
import no.nav.mulighetsrommet.api.avtale.db.PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val avtaleDetaljerDbo = DetaljerDbo(
        sakarkivnummer = SakarkivNummer("24/1234").value,
        navn = "Avtalenavn",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangor = ArrangorDbo(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = listOf(NavIdent("DD1")),
        amoKategorisering = null,
        utdanningslop = null,
        opsjonsmodell = OpsjonsmodellDbo(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3), null),
    )

    val avtalePrismodellDbo = PrismodellDbo(
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
        prisbetingelser = "Alt er dyrt",
    )
    val avtalePersonvernDbo = PersonvernDbo(
        personopplysninger = emptyList(),
        personvernBekreftet = false,
    )

    val avtaleVeilederinfoDbo = VeilederinformasjonDbo(
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        redaksjoneltInnhold = null,
    )

    val avtaleDetaljerRequest = AvtaleDetaljerRequest(
        navn = "Avtalenavn",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
        arrangor = null,
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
        amoKategorisering = null,
        utdanningslop = null,
        opsjonsmodell = Opsjonsmodell(
            maksVarighet = LocalDate.now().plusYears(5),
            type = OpsjonsmodellType.TO_PLUSS_EN,
            customNavn = null,
        ),
    )

    val avtalePrismodellRequest = PrismodellRequest(
        type = PrismodellType.ANNEN_AVTALT_PRIS,
        prisbetingelser = null,
        satser = listOf(),
    )
    val avtalePersonvernRequest = AvtalePersonvernRequest(
        personopplysninger = emptyList(),
        personvernBekreftet = false,
    )

    val avtaleVeilederinfoRequest = AvtaleVeilederinfoRequest(
        navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
        beskrivelse = null,
        faneinnhold = null,
    )

    val oppfolging = AvtaleDbo(
        id = UUID.randomUUID(),
        status = AvtaleStatusType.AKTIV,
        avtalenummer = "2023#1",
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        detaljer = avtaleDetaljerDbo,
        prismodell = avtalePrismodellDbo,
        personvern = avtalePersonvernDbo,
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val oppfolgingMedAvtale = AvtaleDbo(
        id = UUID.randomUUID(),
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        avtalenummer = "2023#1",
        status = AvtaleStatusType.AKTIV,
        detaljer = avtaleDetaljerDbo.copy(
            avtaletype = Avtaletype.AVTALE,
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo,
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val gruppeAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        status = AvtaleStatusType.AKTIV,
        avtalenummer = "2024#8",
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        detaljer = avtaleDetaljerDbo.copy(
            navn = "Gruppe Amo",
            tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo,
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val gruppeFagYrke = AvtaleDbo(
        id = UUID.randomUUID(),
        avtalenummer = "2024#8",
        status = AvtaleStatusType.AKTIV,
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        detaljer = avtaleDetaljerDbo.copy(
            navn = "Gruppe Amo",
            tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo,
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val VTA = AvtaleDbo(
        id = UUID.randomUUID(),
        status = AvtaleStatusType.AKTIV,
        avtalenummer = "2024#1",
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        detaljer = avtaleDetaljerDbo.copy(
            navn = "Avtalenavn for VTA",
            tiltakstypeId = TiltakstypeFixtures.VTA.id,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = OpsjonsmodellDbo(OpsjonsmodellType.VALGFRI_SLUTTDATO, null, null),
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo.copy(
            prisbetingelser = null,
            prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        ),
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val AFT = AvtaleDbo(
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        status = AvtaleStatusType.AKTIV,
        id = UUID.randomUUID(),
        avtalenummer = "2024#1",
        detaljer = avtaleDetaljerDbo.copy(
            navn = "Avtalenavn for AFT",
            tiltakstypeId = TiltakstypeFixtures.AFT.id,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = OpsjonsmodellDbo(OpsjonsmodellType.VALGFRI_SLUTTDATO, null, null),
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo.copy(
            prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
            prisbetingelser = null,
        ),
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val EnkelAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        status = AvtaleStatusType.AKTIV,
        avtalenummer = "2024#1",
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        detaljer = avtaleDetaljerDbo.copy(
            navn = "Avtalenavn for EnkelAmo",
            tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo.copy(
            prisbetingelser = null,
            prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        ),
        veilederinformasjon = VeilederinformasjonDbo(
            navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
            redaksjoneltInnhold = null,
        ),
    )

    val jobbklubb = AvtaleDbo(
        id = UUID.randomUUID(),
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        avtalenummer = "2023#13",
        status = AvtaleStatusType.AKTIV,
        detaljer = avtaleDetaljerDbo.copy(
            navn = "Jobbklubb avtale",
            sakarkivnummer = SakarkivNummer("24/3234").value,
            tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
            avtaletype = Avtaletype.RAMMEAVTALE,
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo,
        veilederinformasjon = avtaleVeilederinfoDbo,
    )

    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        detaljer = avtaleDetaljerRequest,
        prismodell = avtalePrismodellRequest,
        personvern = avtalePersonvernRequest,
        veilederinformasjon = avtaleVeilederinfoRequest,
    )

    /**
     * ARR = ArbeidsrettetRehabilitering
     */
    val ARR = AvtaleDbo(
        id = UUID.randomUUID(),
        avtalenummer = "2023#13",
        status = AvtaleStatusType.AKTIV,
        opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
        detaljer = avtaleDetaljerDbo.copy(
            navn = "ARR avtale",
            sakarkivnummer = SakarkivNummer("24/3234").value,
            tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
        ),
        personvern = avtalePersonvernDbo,
        prismodell = avtalePrismodellDbo,
        veilederinformasjon = avtaleVeilederinfoDbo,
    )
}
