package no.nav.mulighetsrommet.api.persistence.testing.fixture

import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.persistence.gjennomforing.GjennomforingDbo
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

object GjennomforingDboFixtures {
    val Oppfolging1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Oppfølging 1",
        tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = AvtaleFixtures.oppfolging.startDato,
        sluttDato = AvtaleFixtures.oppfolging.sluttDato,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.oppfolging.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.AvtaltPrisPerTimeOppfolging.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val VTA1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "VTA 1",
        tiltakskode = TiltakstypeFixtures.VTA.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.VTA.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.ForhandsgodkjentVtas.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val AFT1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "AFT 1",
        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.AFT.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.ForhandsgodkjentAft.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val TAO = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Varig tilrettelagt arbeid i ordinær virksomhet",
        tiltakskode = TiltakstypeFixtures.TAO.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = null,
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.TAO.id,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.ForhandsgodkjentTao.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val GruppeAmo1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Gruppe Amo 1",
        tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.gruppeAmo.id,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.AnnenAvtaltPris.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val GruppeFagYrke1 = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Gruppe Fag- og yrkesopplæring 1",
        tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2023, 2, 1),
        status = GjennomforingStatusType.GJENNOMFORES,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.gruppeFagYrke.id,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.AnnenAvtaltPris.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val ArbeidsrettetRehabilitering = GjennomforingDbo(
        id = UUID.randomUUID(),
        type = GjennomforingType.AVTALE,
        navn = "Arbeidsretter Rehabilitering 1",
        tiltakskode = TiltakstypeFixtures.ArbeidsrettetRehabilitering.tiltakskode,
        arrangorId = ArrangorFixtures.underenhet1.id,
        startDato = LocalDate.of(2023, 1, 1),
        sluttDato = LocalDate.of(2026, 1, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        antallPlasser = 12,
        avtaleId = AvtaleFixtures.ARR.id,
        oppstart = GjennomforingOppstartstype.FELLES,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        deltidsprosent = 100.0,
        prismodellId = PrismodellFixtures.AnnenAvtaltPris.id,
        ansvarligEnhet = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val EnkelAmo = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakskode = TiltakstypeFixtures.EnkelAmo.tiltakskode,
        type = GjennomforingType.ENKELTPLASS,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Enkelamo 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        status = GjennomforingStatusType.ENKELTPLASS_DELTAR,
        deltidsprosent = 100.0,
        antallPlasser = 1,
        prismodellId = PrismodellFixtures.AnskaffetEnkeltplass.id,
        ansvarligEnhet = NavEnhetNummer("0400"),
        avtaleId = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val EnkelFagOgYrke = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakskode = TiltakstypeFixtures.EnkelFagOgYrke.tiltakskode,
        type = GjennomforingType.ENKELTPLASS,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
        arrangorId = ArrangorFixtures.underenhet2.id,
        navn = "Enkelfag og yrke 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        status = GjennomforingStatusType.ENKELTPLASS_DELTAR,
        deltidsprosent = 100.0,
        antallPlasser = 1,
        prismodellId = PrismodellFixtures.AnskaffetEnkeltplass.id,
        ansvarligEnhet = NavEnhetNummer("0400"),
        avtaleId = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val HoyereUtdanning = GjennomforingDbo(
        id = UUID.fromString("cdc50d11-7d86-4a4b-a8d0-1f8a1be575d0"),
        type = GjennomforingType.ENKELTPLASS,
        tiltakskode = Tiltakskode.HOYERE_UTDANNING,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Høyere utdanning",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 12, 31),
        status = GjennomforingStatusType.ENKELTPLASS_DELTAR,
        deltidsprosent = 100.0,
        antallPlasser = 1,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        ansvarligEnhet = NavEnhetNummer("0400"),
        prismodellId = PrismodellFixtures.TilskuddTilOpplaering.id,
        avtaleId = null,
        arenaTiltaksnummer = null,
        arenaAnsvarligEnhet = null,
    )

    val ArenaEnkelAmo = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakskode = TiltakstypeFixtures.EnkelAmo.tiltakskode,
        type = GjennomforingType.ARENA,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.TRENGER_GODKJENNING,
        arrangorId = ArrangorFixtures.underenhet1.id,
        navn = "Arena Enkelamo 1",
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        status = GjennomforingStatusType.AVSLUTTET,
        deltidsprosent = 100.0,
        antallPlasser = 10,
        arenaTiltaksnummer = Tiltaksnummer("2021#1234"),
        arenaAnsvarligEnhet = "1234",
        avtaleId = null,
        ansvarligEnhet = null,
        prismodellId = null,
    )

    val ArenaArbeidsrettetRehabilitering = GjennomforingDbo(
        id = UUID.randomUUID(),
        tiltakskode = TiltakstypeFixtures.ArbeidsrettetRehabilitering.tiltakskode,
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
        ansvarligEnhet = null,
        avtaleId = null,
        prismodellId = null,
    )
}
