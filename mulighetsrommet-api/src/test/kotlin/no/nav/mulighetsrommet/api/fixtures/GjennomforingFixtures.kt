package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

object GjennomforingFixtures {
    val Oppfolging1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Oppfølging 1",
        tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = AvtaleFixtures.oppfolging.detaljerDbo.startDato,
        sluttDato = AvtaleFixtures.oppfolging.detaljerDbo.sluttDato,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.oppfolging.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        oppmoteSted = "Munch museet",
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorDato = null,
        prismodellId = PrismodellFixtures.AvtaltPrisPerTimeOppfolging.id,
    )

    val VTA1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "VTA 1",
        tiltakstypeId = TiltakstypeFixtures.VTA.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.VTA.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        oppmoteSted = "Oslo",
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorDato = null,
        prismodellId = PrismodellFixtures.ForhandsgodkjentVta.id,
    )

    val AFT1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "AFT 1",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.AFT.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        oppmoteSted = "Oslo",
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorDato = null,
        prismodellId = PrismodellFixtures.ForhandsgodkjentAft.id,
    )

    val GruppeAmo1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Gruppe Amo 1",
        tiltakstypeId = TiltakstypeFixtures.GruppeAmo.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.gruppeAmo.id,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        oppmoteSted = "Oslo",
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorDato = null,
        prismodellId = PrismodellFixtures.AnnenAvtaltPris.id,
    )

    val GruppeFagYrke1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Gruppe Fag- og yrkesopplæring 1",
        tiltakstypeId = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.gruppeFagYrke.id,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        oppmoteSted = "Oslo",
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorDato = null,
        prismodellId = PrismodellFixtures.AnnenAvtaltPris.id,
    )

    val ArbeidsrettetRehabilitering = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Arbeidsretter Rehabilitering 1",
        tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2026, 1, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.ARR.id,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        oppmoteSted = "Oslo",
        faneinnhold = null,
        beskrivelse = null,
        deltidsprosent = 100.0,
        estimertVentetidVerdi = 3,
        estimertVentetidEnhet = "dag",
        tilgjengeligForArrangorDato = null,
        prismodellId = PrismodellFixtures.AnnenAvtaltPris.id,
    )

    val EnkelAmo = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        type = GjennomforingType.ENKELTPLASS,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Enkelamo 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 1,
    )

    val ArenaEnkelAmo = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
        type = GjennomforingType.ARENA,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Arena Enkelamo 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 10,
        arenaTiltaksnummer = Tiltaksnummer("2021#1234"),
        arenaAnsvarligEnhet = "1234",
    )

    val ArenaArbeidsrettetRehabilitering = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakstypeId = TiltakstypeFixtures.ArbeidsrettetRehabilitering.id,
        type = GjennomforingType.ARENA,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Arena ARR",
        startDato = LocalDate.of(2022, 1, 1),
        sluttDato = LocalDate.of(2023, 12, 31),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 10,
        arenaTiltaksnummer = Tiltaksnummer("2022#1"),
        arenaAnsvarligEnhet = "1234",
    )

    fun createGjennomforingRequest(
        avtale: AvtaleDbo,
        id: UUID = UUID.randomUUID(),
        prismodellId: UUID = avtale.prismodeller.single(),
        arrangorId: UUID = ArrangorFixtures.underenhet1.id,
        startDato: LocalDate = avtale.detaljerDbo.startDato,
        sluttDato: LocalDate? = avtale.detaljerDbo.sluttDato,
        oppstart: GjennomforingOppstartstype = GjennomforingOppstartstype.LOPENDE,
        pamelding: GjennomforingPameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        navRegioner: Set<NavEnhetNummer> = setOf(NavEnhetFixtures.Innlandet.enhetsnummer),
        navKontorer: Set<NavEnhetNummer> = setOf(NavEnhetFixtures.Gjovik.enhetsnummer),
        administratorer: Set<NavIdent> = setOf(NavIdent("DD1")),
    ): GjennomforingRequest {
        return GjennomforingRequest(
            id = id,
            tiltakstypeId = avtale.detaljerDbo.tiltakstypeId,
            avtaleId = avtale.id,
            navn = "Gjennomføring for ${avtale.detaljerDbo.navn}",
            startDato = startDato,
            sluttDato = sluttDato,
            oppstart = oppstart,
            pameldingType = pamelding,
            prismodellId = prismodellId,
            arrangorId = arrangorId,
            arrangorKontaktpersoner = setOf(),
            veilederinformasjon = GjennomforingVeilederinfoRequest(
                navRegioner = navRegioner,
                navKontorer = navKontorer,
                navAndreEnheter = setOf(),
                faneinnhold = null,
                beskrivelse = null,
            ),
            kontaktpersoner = setOf(),
            administratorer = administratorer,
            antallPlasser = 1,
            deltidsprosent = 100.0,
            oppmoteSted = null,
            estimertVentetid = null,
            tilgjengeligForArrangorDato = null,
            amoKategorisering = null,
        )
    }
}
