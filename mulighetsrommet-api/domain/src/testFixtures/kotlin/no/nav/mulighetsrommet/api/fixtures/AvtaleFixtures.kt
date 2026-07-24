package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.AvtaleStatus
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

object AvtaleFixtures {
    val defaultArrangor = Avtale.Arrangor(
        hovedenhet = ArrangorFixtures.hovedenhet.id,
        underenheter = listOf(ArrangorFixtures.underenhet1.id),
    )

    val defaultVeilederinfo = Avtale.VeilederInfo(
        beskrivelse = null,
        faneinnhold = null,
        navEnheter = setOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
    )

    val defaultPersonvern = Avtale.Personvern(
        personopplysninger = emptySet(),
        annetBeskrivelse = null,
        erBekreftet = false,
    )

    val defaultOpsjoner = Avtale.Opsjoner(
        modell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        registreringer = listOf(),
    )

    val oppfolging: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.OPPFOLGING,
        navn = "Avtalenavn",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.RAMMEAVTALE,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = null,
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging)),
    )

    val gruppeAmo: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        navn = "Gruppe Amo",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = OpplaringKategorisering(kurstype = KurstypeFixtures.studiespesialisering.id),
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris)),
    )

    val gruppeFagYrke: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        navn = "Fag Yrke",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = null,
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris)),
    )

    val VTA: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        navn = "Avtalenavn for VTA",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = null,
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Systembestemt(PrismodellFixtures.ForhandsgodkjentVtas),
    )

    val AFT: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        navn = "Avtalenavn for AFT",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = null,
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Systembestemt(PrismodellFixtures.ForhandsgodkjentAft),
    )

    val TAO: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER,
        navn = "Avtalenavn for TAO",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/1234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = null,
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Systembestemt(PrismodellFixtures.ForhandsgodkjentTao),
    )

    val ARR: Avtale = Avtale(
        id = UUID.randomUUID(),
        tiltakskode = Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        navn = "ARR avtale",
        avtalenummer = null,
        sakarkivNummer = SakarkivNummer("24/3234"),
        arrangor = defaultArrangor,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        avtaletype = Avtaletype.RAMMEAVTALE,
        status = AvtaleStatus.Aktiv,
        administratorer = setOf(NavIdent("DD1")),
        veilederinfo = defaultVeilederinfo,
        personvern = defaultPersonvern,
        opplaring = null,
        opsjoner = defaultOpsjoner,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris)),
    )
}
