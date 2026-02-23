package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.amo.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

object AvtaleFixtures {
    fun detaljerDbo(): DetaljerDbo = DetaljerDbo(
        navn = "Avtalenavn",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangor = ArrangorDbo(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = listOf(NavIdent("DD1")),
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
    )

    fun personvernDbo(
        personopplysninger: List<Personopplysning> = emptyList(),
        personvernBekreftet: Boolean = false,
    ): PersonvernDbo = PersonvernDbo(
        personopplysninger = personopplysninger,
        personvernBekreftet = personvernBekreftet,
    )

    fun veilederinformasjonDbo(
        navEnheter: Set<NavEnhetNummer> = setOf(
            NavEnhetNummer("0400"),
            NavEnhetNummer("0502"),
        ),
    ): VeilederinformasjonDbo = VeilederinformasjonDbo(
        navEnheter = navEnheter,
        redaksjoneltInnhold = null,
    )

    val oppfolging: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
        prismodeller = listOf(PrismodellFixtures.AvtaltPrisPerTimeOppfolging.id),
    )

    val gruppeAmo: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
            navn = "Gruppe Amo",
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        ),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        prismodeller = listOf(PrismodellFixtures.AnnenAvtaltPris.id),
    )

    val gruppeFagYrke: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Fag Yrke",
            tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,

        ),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        prismodeller = listOf(PrismodellFixtures.AnnenAvtaltPris.id),
    )

    val VTA: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for VTA",
            tiltakstypeId = TiltakstypeFixtures.VTA.id,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
        prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentVta.id),
    )

    val AFT: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for AFT",
            tiltakstypeId = TiltakstypeFixtures.AFT.id,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
        prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentAft.id),
    )

    val EnkelAmo: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for EnkelAmo",
            tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,

        ),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        prismodeller = listOf(),
    )

    val ARR: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "ARR avtale",
            tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
            sakarkivNummer = SakarkivNummer("24/3234"),
        ),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        prismodeller = listOf(PrismodellFixtures.AnnenAvtaltPris.id),
    )

    fun createAvtaleRequest(
        tiltakskode: Tiltakskode,
        avtaletype: Avtaletype = Avtaletype.RAMMEAVTALE,
        arrangor: DetaljerRequest.Arrangor = DetaljerRequest.Arrangor(
            hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
            underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
            kontaktpersoner = emptyList(),
        ),
        administratorer: List<NavIdent> = listOf(NavAnsattFixture.DonaldDuck.navIdent),
        prismodell: List<PrismodellDbo> = listOf(PrismodellFixtures.AnnenAvtaltPris),
        opsjonsmodell: Opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        amo: AmoKategoriseringRequest? = null,
    ): OpprettAvtaleRequest {
        return OpprettAvtaleRequest(
            id = UUID.randomUUID(),
            detaljer = DetaljerRequest(
                navn = "Avtale",
                tiltakskode = tiltakskode,
                arrangor = arrangor,
                sakarkivNummer = SakarkivNummer("24/1234"),
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().plusMonths(1),
                administratorer = administratorer,
                avtaletype = avtaletype,
                amoKategorisering = amo,
                opsjonsmodell = opsjonsmodell,
                utdanningslop = null,
            ),
            veilederinformasjon = VeilederinfoRequest(
                navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
                beskrivelse = null,
                faneinnhold = null,
            ),
            personvern = PersonvernRequest(
                personopplysninger = emptyList(),
                personvernBekreftet = false,
            ),
            prismodeller = prismodell.map { prismodell ->
                PrismodellRequest(
                    id = prismodell.id,
                    type = prismodell.type,
                    valuta = Valuta.NOK,
                    prisbetingelser = prismodell.prisbetingelser,
                    satser = (prismodell.satser ?: listOf()).map {
                        AvtaltSatsRequest(it.gjelderFra, it.sats.belop)
                    },
                )
            },
        )
    }
}
