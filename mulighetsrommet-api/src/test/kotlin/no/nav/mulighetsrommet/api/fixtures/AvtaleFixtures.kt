package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.model.*
import java.time.LocalDate
import java.util.*

object AvtaleFixtures {
    val oppfolging = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.RAMMEAVTALE,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    val oppfolgingMedAvtale = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.AVTALE,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    val gruppeAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Amo",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = AmoKategorisering.Studiespesialisering,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    val gruppeFagYrke = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Gruppe Amo",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    val VTA = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for VTA",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.VTA.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        utdanningslop = null,
        prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        satser = listOf(),
    )

    val AFT = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for AFT",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        utdanningslop = null,
        prismodell = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        satser = listOf(),
    )

    val EnkelAmo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtalenavn for EnkelAmo",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        prisbetingelser = null,
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    val jobbklubb = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Jobbklubb avtale",
        sakarkivNummer = SakarkivNummer("24/3234"),
        tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.RAMMEAVTALE,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )

    val avtaleRequest = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtalenavn",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
        arrangor = null,
        startDato = LocalDate.of(2023, 1, 11),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
        veilederinformasjon = VeilederinfoRequest(
            navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
            beskrivelse = null,
            faneinnhold = null,
        ),
        personvern = PersonvernRequest(
            personopplysninger = emptyList(),
            personvernBekreftet = false,
        ),
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(
            opsjonMaksVarighet = LocalDate.now().plusYears(5),
            type = OpsjonsmodellType.TO_PLUSS_EN,
            customOpsjonsmodellNavn = null,
        ),
        utdanningslop = null,
        prismodell = PrismodellRequest(
            type = PrismodellType.ANNEN_AVTALT_PRIS,
            prisbetingelser = null,
            satser = listOf(),
        ),
    )

    /**
     * ARR = ArbeidsrettetRehabilitering
     */
    val ARR = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "ARR avtale",
        sakarkivNummer = SakarkivNummer("24/3234"),
        tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.RAMMEAVTALE,
        prisbetingelser = "Alt er dyrt",
        administratorer = listOf(NavIdent("DD1")),
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodell = PrismodellType.ANNEN_AVTALT_PRIS,
        satser = listOf(),
    )
}
