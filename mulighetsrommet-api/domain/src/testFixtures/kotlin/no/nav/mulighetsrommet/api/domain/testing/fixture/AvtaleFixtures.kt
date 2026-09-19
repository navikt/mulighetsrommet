package no.nav.mulighetsrommet.api.domain.testing.fixture

import arrow.core.getOrElse
import no.nav.mulighetsrommet.api.domain.avtale.Avtale
import no.nav.mulighetsrommet.api.domain.avtale.OpprettAvtaleCommand
import no.nav.mulighetsrommet.api.domain.avtale.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.avtale.Vurderingsdato
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

object AvtaleFixtures {
    val defaultStartDato: LocalDate = LocalDate.of(2023, 1, 1)

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

    private fun opprett(
        tiltakskode: Tiltakskode,
        navn: String,
        arrangor: Avtale.Arrangor?,
        avtaletype: Avtaletype,
        opplaring: OpplaringKategorisering?,
        prisinfo: Avtale.Prisinfo,
        opsjonsmodell: Opsjonsmodell = Opsjonsmodell.IngenOpsjonsmulighet,
        startDato: LocalDate = defaultStartDato,
        sluttDato: LocalDate? = LocalDate.now().plusMonths(3),
    ): Avtale = Avtale.opprett(
        command = OpprettAvtaleCommand(
            id = UUID.randomUUID(),
            tiltakskode = tiltakskode,
            navn = navn,
            sakarkivNummer = SakarkivNummer("24/1234"),
            arrangor = arrangor,
            startDato = startDato,
            sluttDato = sluttDato,
            opsjonsmodell = opsjonsmodell,
            avtaletype = avtaletype,
            administratorer = setOf(NavIdent("DD1")),
            veilederinfo = defaultVeilederinfo,
            personvern = defaultPersonvern,
            opplaring = opplaring,
            prisinfo = prisinfo,
        ),
        vurderingsdato = Vurderingsdato(LocalDate.now()),
    ).getOrElse { error("Ugyldig testfixture for avtale \"$navn\": $it") }

    val oppfolging: Avtale = opprett(
        tiltakskode = Tiltakskode.OPPFOLGING,
        navn = "Avtalenavn",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.RAMMEAVTALE,
        opplaring = null,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging)),
    )

    val gruppeAmo: Avtale = opprett(
        tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        navn = "Gruppe Amo",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        opplaring = OpplaringKategorisering(kurstype = KurstypeFixtures.studiespesialisering.id),
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris)),
    )

    val gruppeFagYrke: Avtale = opprett(
        tiltakskode = Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        navn = "Fag Yrke",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
        opplaring = null,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris)),
    )

    val VTA: Avtale = opprett(
        tiltakskode = Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        navn = "Avtalenavn for VTA",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        opplaring = null,
        prisinfo = Avtale.Prisinfo.Systembestemt(PrismodellFixtures.ForhandsgodkjentVtas),
        opsjonsmodell = Opsjonsmodell.ValgfriSluttdato,
    )

    val AFT: Avtale = opprett(
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        navn = "Avtalenavn for AFT",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        opplaring = null,
        prisinfo = Avtale.Prisinfo.Systembestemt(PrismodellFixtures.ForhandsgodkjentAft),
        opsjonsmodell = Opsjonsmodell.ValgfriSluttdato,
        sluttDato = null,
    )

    val TAO: Avtale = opprett(
        tiltakskode = Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER,
        navn = "Avtalenavn for TAO",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.FORHANDSGODKJENT,
        opplaring = null,
        prisinfo = Avtale.Prisinfo.Systembestemt(PrismodellFixtures.ForhandsgodkjentTao),
        opsjonsmodell = Opsjonsmodell.ValgfriSluttdato,
        sluttDato = null,
    )

    val ARR: Avtale = opprett(
        tiltakskode = Tiltakskode.ARBEIDSRETTET_REHABILITERING,
        navn = "ARR avtale",
        arrangor = defaultArrangor,
        avtaletype = Avtaletype.RAMMEAVTALE,
        opplaring = null,
        prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris)),
    )
}
