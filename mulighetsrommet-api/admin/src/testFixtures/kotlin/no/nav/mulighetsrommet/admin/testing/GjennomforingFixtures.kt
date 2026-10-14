package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingRequest
import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingVeilederinfoRequest
import no.nav.mulighetsrommet.api.domain.avtale.Avtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplassStatus
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingKompakt
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object GjennomforingFixtures {
    fun createGjennomforingAvtale(
        id: UUID = UUID.randomUUID(),
        navn: String = "Test gjennomføring",
        tiltakskode: Tiltakskode = Tiltakskode.OPPFOLGING,
        prismodell: Prismodell = PrismodellFixtures.AnnenAvtaltPris,
        startDato: LocalDate = LocalDate.of(2025, 1, 1),
        sluttDato: LocalDate? = LocalDate.of(2025, 12, 31),
        opprettetTidspunkt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
    ) = GjennomforingAvtale(
        id = id,
        tiltakstype = Gjennomforing.Tiltakstype(UUID.randomUUID(), "Oppfølging", tiltakskode),
        lopenummer = Tiltaksnummer("2025/10000"),
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Test arrangør",
            slettet = false,
        ),
        arena = null,
        navn = navn,
        status = GjennomforingAvtaleStatus.Gjennomfores,
        startDato = startDato,
        sluttDato = sluttDato,
        deltidsprosent = 100.0,
        antallPlasser = 10,
        opprettetTidspunkt = opprettetTidspunkt,
        oppdatertTidspunkt = opprettetTidspunkt,
        prismodell = prismodell,
        oppstart = GjennomforingOppstartstype.LOPENDE,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        avtaleId = UUID.randomUUID(),
        kontorstruktur = emptyList(),
        apentForPamelding = true,
        stengt = emptyList(),
    )

    fun createGjennomforingEnkeltplass(
        id: UUID = UUID.randomUUID(),
        tiltakskode: Tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        prismodell: Prismodell = PrismodellFixtures.AnskaffetEnkeltplass,
        startDato: LocalDate? = LocalDate.of(2025, 1, 1),
        sluttDato: LocalDate? = LocalDate.of(2025, 12, 31),
        opprettetTidspunkt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
    ) = GjennomforingEnkeltplass(
        id = id,
        lopenummer = Tiltaksnummer("2025/10001"),
        tiltakstype = Gjennomforing.Tiltakstype(UUID.randomUUID(), "Enkel AMO", tiltakskode),
        arrangor = Gjennomforing.ArrangorUnderenhet(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("123456789"),
            navn = "Test arrangør",
            slettet = false,
        ),
        arena = null,
        navn = "Enkel AMO",
        status = GjennomforingEnkeltplassStatus.Deltar,
        startDato = startDato,
        sluttDato = sluttDato,
        deltidsprosent = 100.0,
        antallPlasser = 1,
        opprettetTidspunkt = opprettetTidspunkt,
        oppdatertTidspunkt = opprettetTidspunkt,
        prismodell = prismodell,
        oppstart = GjennomforingOppstartstype.ENKELTPLASS,
        pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        ansvarligEnhet = GjennomforingEnkeltplass.AnsvarligEnhet(
            enhetsnummer = NavEnhetNummer("0400"),
            navn = "Nav Innlandet",
        ),
    )

    fun createGjennomforingAvtaleDetaljer() = GjennomforingAvtaleDetaljer(
        publisert = true,
        beskrivelse = null,
        faneinnhold = null,
        kontorstruktur = emptyList(),
        kontaktpersoner = emptyList(),
        oppmoteSted = null,
        estimertVentetid = null,
        administratorer = emptyList(),
        opplaringKategorisering = null,
        tilgjengeligForArrangorDato = null,
        arrangorKontaktpersoner = emptyList(),
    )

    fun createGjennomforingAvtaleKompakt(
        id: UUID = UUID.randomUUID(),
        navn: String = "Test gjennomføring",
        startDato: LocalDate = LocalDate.of(2025, 1, 1),
        sluttDato: LocalDate? = LocalDate.of(2025, 12, 31),
    ) = GjennomforingAvtaleKompakt(
        id = id,
        lopenummer = Tiltaksnummer("2025/10000"),
        tiltakstype = GjennomforingKompakt.Tiltakstype(
            UUID.randomUUID(),
            "Oppfølging",
            Tiltakskode.OPPFOLGING,
        ),
        arrangor = GjennomforingKompakt.ArrangorUnderenhet(
            UUID.randomUUID(),
            Organisasjonsnummer("123456789"),
            "Test arrangør",
        ),
        startDato = startDato,
        sluttDato = sluttDato,
        status = GjennomforingAvtaleStatus.Gjennomfores,
        navn = navn,
        kontorstruktur = emptyList(),
        publisert = true,
    )

    fun createGjennomforingEnkeltplassKompakt(
        id: UUID = UUID.randomUUID(),
        tiltakskode: Tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        startDato: LocalDate? = LocalDate.of(2025, 1, 1),
        sluttDato: LocalDate? = LocalDate.of(2025, 12, 31),
    ) = GjennomforingEnkeltplassKompakt(
        id = id,
        lopenummer = Tiltaksnummer("2025/10001"),
        tiltakstype = GjennomforingKompakt.Tiltakstype(
            UUID.randomUUID(),
            "Enkel AMO",
            tiltakskode,
        ),
        arrangor = GjennomforingKompakt.ArrangorUnderenhet(
            UUID.randomUUID(),
            Organisasjonsnummer("123456789"),
            "Test arrangør",
        ),
        startDato = startDato,
        sluttDato = sluttDato,
        status = GjennomforingEnkeltplassStatus.Deltar,
    )

    fun createGjennomforingRequest(
        avtale: Avtale,
        id: UUID = UUID.randomUUID(),
        prismodellId: UUID = avtale.prisinfo.toList().single().id,
        arrangorId: UUID = ArrangorFixtures.underenhet1.id,
        startDato: LocalDate = avtale.startDato,
        sluttDato: LocalDate? = avtale.sluttDato,
        oppstart: GjennomforingOppstartstype = GjennomforingOppstartstype.LOPENDE,
        pamelding: GjennomforingPameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
        navRegioner: Set<NavEnhetNummer> = setOf(NavEnhetFixtures.Innlandet.enhetsnummer),
        navKontorer: Set<NavEnhetNummer> = setOf(NavEnhetFixtures.Gjovik.enhetsnummer),
        administratorer: Set<NavIdent> = setOf(NavIdent("DD1")),
    ): GjennomforingRequest {
        return GjennomforingRequest(
            id = id,
            avtaleId = avtale.id,
            detaljer = GjennomforingDetaljerRequest(
                navn = "Gjennomføring for ${avtale.navn}",
                startDato = startDato,
                sluttDato = sluttDato,
                oppstart = oppstart,
                pameldingType = pamelding,
                prismodellId = prismodellId,
                arrangorId = arrangorId,
                arrangorKontaktpersoner = setOf(),
                administratorer = administratorer,
                antallPlasser = 1,
                deltidsprosent = 100.0,
                oppmoteSted = null,
                tilgjengeligForArrangorDato = null,
                amoKategorisering = null,
            ),
            veilederinformasjon = GjennomforingVeilederinfoRequest(
                navRegioner = navRegioner,
                navKontorer = navKontorer,
                navAndreEnheter = setOf(),
                faneinnhold = null,
                beskrivelse = null,
                kontaktpersoner = emptySet(),
            ),
        )
    }
}
