package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.admin.gjennomforing.GjennomforingRequest
import no.nav.mulighetsrommet.api.domain.avtale.Avtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.AnsvarligEnhet
import no.nav.mulighetsrommet.api.domain.gjennomforing.Gjennomforing
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtale
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleStatus
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.persistence.testing.fixture.GjennomforingDboFixtures
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.mulighetsrommet.admin.testing.GjennomforingFixtures as AdminGjennomforingFixtures

object GjennomforingFixtures {
    val Oppfolging1 = GjennomforingDboFixtures.Oppfolging1
    val VTA1 = GjennomforingDboFixtures.VTA1
    val AFT1 = GjennomforingDboFixtures.AFT1
    val TAO = GjennomforingDboFixtures.TAO
    val GruppeAmo1 = GjennomforingDboFixtures.GruppeAmo1
    val GruppeFagYrke1 = GjennomforingDboFixtures.GruppeFagYrke1
    val ArbeidsrettetRehabilitering = GjennomforingDboFixtures.ArbeidsrettetRehabilitering
    val EnkelAmo = GjennomforingDboFixtures.EnkelAmo
    val EnkelFagOgYrke = GjennomforingDboFixtures.EnkelFagOgYrke
    val HoyereUtdanning = GjennomforingDboFixtures.HoyereUtdanning
    val ArenaEnkelAmo = GjennomforingDboFixtures.ArenaEnkelAmo
    val ArenaArbeidsrettetRehabilitering = GjennomforingDboFixtures.ArenaArbeidsrettetRehabilitering

    fun createGjennomforingAvtale(
        id: UUID,
        periode: Periode,
        tiltakskode: Tiltakskode,
        prismodell: Prismodell = Prismodell.AnnenAvtaltPris(
            id = PrismodellFixtures.AnnenAvtaltPris.id,
            valuta = Valuta.NOK,
            tilsagnPerDeltaker = false,
            prisbetingelser = null,
        ),
        opprettetTidspunkt: Instant = Instant.now(),
    ): GjennomforingAvtale {
        return GjennomforingAvtale(
            id = id,
            tiltakstype = Gjennomforing.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Test tiltakstype",
                tiltakskode = tiltakskode,
            ),
            lopenummer = Tiltaksnummer("2025/10000"),
            arrangor = Gjennomforing.ArrangorUnderenhet(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "Test arrangør",
                slettet = false,
            ),
            arena = null,
            navn = tiltakskode.name,
            startDato = periode.start,
            sluttDato = periode.getLastInclusiveDate(),
            deltidsprosent = 100.0,
            antallPlasser = 10,
            status = GjennomforingAvtaleStatus.Gjennomfores,
            apentForPamelding = true,
            avtaleId = UUID.randomUUID(),
            ansvarligEnhet = AnsvarligEnhet(NavEnhetNummer("0400"), "Nav Innlandet"),
            kontorstruktur = emptyList(),
            oppstart = GjennomforingOppstartstype.LOPENDE,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            opprettetTidspunkt = opprettetTidspunkt,
            oppdatertTidspunkt = opprettetTidspunkt,
            stengt = listOf(),
            prismodell = prismodell,
        )
    }

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
    ): GjennomforingRequest = AdminGjennomforingFixtures.createGjennomforingRequest(
        avtale = avtale,
        id = id,
        prismodellId = prismodellId,
        arrangorId = arrangorId,
        startDato = startDato,
        sluttDato = sluttDato,
        oppstart = oppstart,
        pamelding = pamelding,
        navRegioner = navRegioner,
        navKontorer = navKontorer,
        administratorer = administratorer,
    )
}
