package no.nav.mulighetsrommet.api.fixtures

import PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
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

    fun prismodellDbo(
        id: UUID = UUID.randomUUID(),
        prismodellType: PrismodellType = PrismodellType.ANNEN_AVTALT_PRIS,
        prisbetingelser: String = "Alt er dyrt",
        satser: List<AvtaltSats> = emptyList(),
    ): PrismodellDbo = PrismodellDbo(
        id = id,
        type = prismodellType,
        prisbetingelser = prisbetingelser,
        satser = satser,
    )

    val oppfolging: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo(),
        prismodellDbo = prismodellDbo(
            prismodellType = PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
            satser = listOf(AvtaltSats(gjelderFra = detaljerDbo().startDato, sats = 1234)),
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
    )

    val oppfolgingMedAvtale: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(avtaletype = Avtaletype.AVTALE),
        prismodellDbo = prismodellDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
    )

    val gruppeAmo: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
            navn = "Gruppe Amo",
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,
            amoKategorisering = AmoKategorisering.Studiespesialisering,
        ),
        prismodellDbo = prismodellDbo(),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
    )

    val gruppeFagYrke: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Gruppe Amo",
            tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
            avtaletype = Avtaletype.OFFENTLIG_OFFENTLIG,

        ),
        prismodellDbo = prismodellDbo(),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
    )
    val VTA: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for VTA",
            tiltakstypeId = TiltakstypeFixtures.VTA.id,
            avtaletype = Avtaletype.FORHANDSGODKJENT,
            opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.VALGFRI_SLUTTDATO, null),

        ),
        prismodellDbo = prismodellDbo(
            prismodellType = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
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
        prismodellDbo = prismodellDbo(
            prismodellType = PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
        ),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        personvernDbo = personvernDbo(),
    )

    val EnkelAmo: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Avtalenavn for EnkelAmo",
            tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
            sluttDato = null,
            avtaletype = Avtaletype.FORHANDSGODKJENT,

        ),
        prismodellDbo = prismodellDbo(),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
    )

    val jobbklubb: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "Jobbklubb avtale",
            tiltakstypeId = TiltakstypeFixtures.Jobbklubb.id,

        ),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
        prismodellDbo = prismodellDbo(),
    )

    val opprettAvtaleRequest: OpprettAvtaleRequest = OpprettAvtaleRequest(
        id = UUID.randomUUID(),
        detaljer = DetaljerRequest(
            navn = "Avtalenavn",
            sakarkivNummer = SakarkivNummer("24/1234"),
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
            arrangor = DetaljerRequest.Arrangor(
                hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
                underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                kontaktpersoner = emptyList(),
            ),
            startDato = LocalDate.of(2023, 1, 11),
            sluttDato = LocalDate.now().plusMonths(3),
            avtaletype = Avtaletype.RAMMEAVTALE,
            administratorer = listOf(NavAnsattFixture.DonaldDuck.navIdent),
            amoKategorisering = null,
            opsjonsmodell = Opsjonsmodell(
                opsjonMaksVarighet = LocalDate.now().plusYears(5),
                type = OpsjonsmodellType.TO_PLUSS_EN,
                customOpsjonsmodellNavn = null,
            ),
            utdanningslop = null,
        ),
        veilederinformasjon = VeilederinfoRequest(
            navEnheter = listOf(NavEnhetFixtures.Innlandet.enhetsnummer, NavEnhetFixtures.Gjovik.enhetsnummer),
            beskrivelse = null,
            faneinnhold = null,
        ),
        personvern = PersonvernRequest(
            personopplysninger = emptyList(),
            personvernBekreftet = false,
        ),
        prismodell = PrismodellRequest(
            UUID.randomUUID(),
            type = PrismodellType.ANNEN_AVTALT_PRIS,
            prisbetingelser = null,
            satser = listOf(),
        ),
    )

    /**
     * ARR = ArbeidsrettetRehabilitering
     */
    val ARR: AvtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        detaljerDbo = detaljerDbo().copy(
            navn = "ARR avtale",
            tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
            sakarkivNummer = SakarkivNummer("24/3234"),
        ),
        prismodellDbo = prismodellDbo(),
        personvernDbo = personvernDbo(),
        veilederinformasjonDbo = veilederinformasjonDbo(),
    )
}
