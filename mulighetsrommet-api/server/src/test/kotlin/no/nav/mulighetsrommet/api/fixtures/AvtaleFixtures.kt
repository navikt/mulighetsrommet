package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.api.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.persistence.tiltak.AvtaleArrangorDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.AvtaleDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.DetaljerDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.PersonvernDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.VeilederinformasjonDbo
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

object AvtaleFixtures {
    fun detaljerDbo(): DetaljerDbo = DetaljerDbo(
        navn = "Avtalenavn",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        arrangor = AvtaleArrangorDbo(
            hovedenhet = ArrangorFixtures.hovedenhet.id,
            underenheter = listOf(ArrangorFixtures.underenhet1.id),
            kontaktpersoner = emptyList(),
        ),
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.now().plusMonths(3),
        status = AvtaleStatusType.AKTIV,
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = listOf(NavIdent("DD1")),
        opplaringKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
    )

    fun personvernDbo(
        personopplysninger: List<Personopplysning.Type> = emptyList(),
        personvernBekreftet: Boolean = false,
    ): PersonvernDbo = PersonvernDbo(
        personopplysninger = personopplysninger,
        annetChecked = false,
        annetBeskrivelse = null,
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
            tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode,
            navn = "Gruppe Amo",
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            opplaringKategorisering = OpplaringKategorisering(kurstype = KurstypeFixtures.studiespesialisering.id),
        ),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        prismodeller = listOf(PrismodellFixtures.AnnenAvtaltPris.id),
    )

    val gruppeFagYrke: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Fag Yrke",
            tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode,
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
            tiltakskode = TiltakstypeFixtures.VTA.tiltakskode,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
        prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentVtas.id),
    )

    val AFT: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for AFT",
            tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
        prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentAft.id),
    )

    val TAO: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for TAO",
            tiltakskode = TiltakstypeFixtures.TAO.tiltakskode,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
        prismodeller = listOf(PrismodellFixtures.ForhandsgodkjentTao.id),
    )

    val EnkelAmo: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for EnkelAmo",
            tiltakskode = TiltakstypeFixtures.EnkelAmo.tiltakskode,
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
            tiltakskode = TiltakstypeFixtures.ArbeidsrettetRehabilitering.tiltakskode,
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
        prismodell: List<Prismodell> = listOf(PrismodellFixtures.AnnenAvtaltPris),
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
                annetChecked = false,
                annetBeskrivelse = null,
            ),
            prismodeller = prismodell.map { it.toPrismodellRequest() },
        )
    }
}

private fun Prismodell.toPrismodellRequest(): PrismodellRequest = PrismodellRequest(
    id = id,
    type = type,
    valuta = valuta,
    prisbetingelser = when (this) {
        is Prismodell.AnnenAvtaltPris -> prisbetingelser
        is Prismodell.AvtaltPrisPerManedsverk -> prisbetingelser
        is Prismodell.AvtaltPrisPerUkesverk -> prisbetingelser
        is Prismodell.AvtaltPrisPerHeleUkesverk -> prisbetingelser
        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
        is Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
        is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> null
        is Prismodell.TilskuddTilOpplaering -> tilleggsopplysninger
        is Prismodell.IngenKostnader -> tilleggsopplysninger
    },
    satser = satser().map { AvtaltSatsRequest(it.gjelderFra, it.sats.belop) },
    tilsagnPerDeltaker = (this as? Prismodell.AnnenAvtaltPris)?.tilsagnPerDeltaker ?: false,
)
