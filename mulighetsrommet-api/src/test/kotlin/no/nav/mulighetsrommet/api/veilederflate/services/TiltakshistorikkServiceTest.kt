package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.Either
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.ApiDatabaseTestListener
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.clients.amtDeltaker.*
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk.Arrangor
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk.Gjennomforing
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val tiltaksgjennomforing = TiltaksgjennomforingFixtures.Oppfolging1

    val tiltakshistorikkOppfolging = Tiltakshistorikk.GruppetiltakDeltakelse(
        id = UUID.randomUUID(),
        gjennomforing = Gjennomforing(
            id = tiltaksgjennomforing.id,
            navn = tiltaksgjennomforing.navn,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
        ),
        norskIdent = NorskIdent("12345678910"),
        status = DeltakerStatus(
            type = DeltakerStatus.Type.VENTELISTE,
            opprettetDato = LocalDateTime.of(2018, 12, 3, 0, 0),
            aarsak = null,
        ),
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arrangor = Arrangor(ArrangorFixtures.underenhet1.organisasjonsnummer),
    )

    val tiltakshistorikkAvklaring = Tiltakshistorikk.ArenaDeltakelse(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.VENTELISTE,
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arenaTiltakskode = TiltakstypeFixtures.Avklaring.arenaKode,
        beskrivelse = "Avklaring",
        arrangor = Arrangor(Organisasjonsnummer("123456789")),
    )

    val tiltakshistorikkArbeidstrening = Tiltakshistorikk.ArbeidsgiverAvtale(
        norskIdent = NorskIdent("12345678910"),
        startDato = LocalDate.of(2020, 1, 1),
        sluttDato = LocalDate.of(2021, 12, 31),
        avtaleId = UUID.randomUUID(),
        tiltakstype = Tiltakshistorikk.ArbeidsgiverAvtale.Tiltakstype.ARBEIDSTRENING,
        status = ArbeidsgiverAvtaleStatus.GJENNOMFORES,
        arbeidsgiver = Tiltakshistorikk.Arbeidsgiver(ArrangorFixtures.underenhet2.organisasjonsnummer),
    )

    val deltakelseOppfolgingFraKomet = DeltakelseFraKomet(
        deltakerId = tiltakshistorikkOppfolging.id,
        deltakerlisteId = tiltakshistorikkOppfolging.gjennomforing.id,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstype = DeltakelserResponse.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            tiltakskode = GruppeTiltakstype.INDOPPFAG,
        ),
        status = DeltakelseFraKomet.Status(
            type = DeltakerStatus.Type.VENTELISTE,
            visningstekst = "Venteliste",
            aarsak = null,
        ),
        periode = Periode(
            startdato = LocalDate.of(2019, 1, 1),
            sluttdato = LocalDate.of(2019, 12, 3),
        ),
        innsoktDato = LocalDate.of(2018, 12, 3),
        sistEndretDato = LocalDate.of(2018, 12, 5),
    )

    val deltakelseOppfolging = Deltakelse.DeltakelseGruppetiltak(
        id = tiltakshistorikkOppfolging.id,
        gjennomforingId = tiltakshistorikkOppfolging.gjennomforing.id,
        eierskap = Deltakelse.Eierskap.TEAM_KOMET,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstypeNavn = TiltakstypeFixtures.Oppfolging.navn,
        status = Deltakelse.DeltakelseGruppetiltak.Status(
            type = DeltakerStatus.Type.VENTELISTE,
            visningstekst = "Venteliste",
            aarsak = null,
        ),
        periode = Deltakelse.Periode(
            startDato = LocalDate.of(2019, 1, 1),
            sluttDato = LocalDate.of(2019, 12, 3),
        ),
        sistEndretDato = LocalDate.of(2018, 12, 5),
        innsoktDato = LocalDate.of(2018, 12, 3),
    )
    val deltakelseAvklaring = Deltakelse.DeltakelseArena(
        id = tiltakshistorikkAvklaring.id,
        eierskap = Deltakelse.Eierskap.ARENA,
        tittel = "Avklaring hos Hovedenhet AS",
        tiltakstypeNavn = TiltakstypeFixtures.Avklaring.navn,
        status = Deltakelse.DeltakelseArena.Status(
            type = ArenaDeltakerStatus.VENTELISTE,
            visningstekst = "Venteliste",
        ),
        periode = Deltakelse.Periode(
            startDato = LocalDate.of(2018, 12, 3),
            sluttDato = LocalDate.of(2019, 12, 3),
        ),
        sistEndretDato = null,
        innsoktDato = null,
    )
    val deltakelseArbeidstrening = Deltakelse.DeltakelseArbeidsgiverAvtale(
        id = tiltakshistorikkArbeidstrening.avtaleId,
        eierskap = Deltakelse.Eierskap.TEAM_TILTAK,
        tittel = "Arbeidstrening hos Underenhet 2 AS",
        tiltakstypeNavn = "Arbeidstrening",
        status = Deltakelse.DeltakelseArbeidsgiverAvtale.Status(
            type = ArbeidsgiverAvtaleStatus.GJENNOMFORES,
            visningstekst = "Gjennomføres",
        ),
        periode = Deltakelse.Periode(
            startDato = LocalDate.of(2020, 1, 1),
            sluttDato = LocalDate.of(2021, 12, 31),
        ),
        sistEndretDato = null,
        innsoktDato = null,
    )

    val pdlClient: PdlClient = mockk()
    val tiltakshistorikkClient: TiltakshistorikkClient = mockk()
    val amtDeltakerClient: AmtDeltakerClient = mockk()

    fun createTiltakshistorikkService() = TiltakshistorikkService(
        pdlClient = pdlClient,
        tiltakstypeService = TiltakstypeService(database.db),
        amtDeltakerClient = amtDeltakerClient,
        tiltakshistorikkClient = tiltakshistorikkClient,
        arrangorService = ArrangorService(database.db, mockk()),
    )

    coEvery { pdlClient.hentHistoriskeIdenter(any(), any()) } returns listOf(
        IdentInformasjon(
            ident = PdlIdent("12345678910"),
            gruppe = IdentGruppe.FOLKEREGISTERIDENT,
            historisk = false,
        ),
    ).right()

    beforeAny {
        MulighetsrommetTestDomain(
            arrangorer = listOf(
                ArrangorFixtures.hovedenhet,
                ArrangorFixtures.underenhet1,
                ArrangorFixtures.underenhet2,
            ),
            tiltakstyper = listOf(
                TiltakstypeFixtures.Oppfolging,
                TiltakstypeFixtures.Avklaring,
                TiltakstypeFixtures.Arbeidstrening,
            ),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(tiltaksgjennomforing),
        ).initialize(database.db)
    }

    test("henter historikk for bruker basert på person id") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(tiltakshistorikkOppfolging, tiltakshistorikkAvklaring, tiltakshistorikkArbeidstrening),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(deltakelseOppfolgingFraKomet),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(deltakelseArbeidstrening, deltakelseOppfolging, deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    test("inkluderer deltakelser fra komet når de ikke finnes i tiltakshistorikken") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(tiltakshistorikkAvklaring),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(deltakelseOppfolgingFraKomet),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(deltakelseOppfolging, deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    test("viser kun deltakelser fra tiltakshistorikken når det ikke returneres deltakelser fra komet") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(tiltakshistorikkAvklaring),
            meldinger = setOf(),
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(deltakelseAvklaring),
            historiske = emptyList(),
        )
    }

    test("sorterer deltakelser basert nyeste startdato") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(tiltakshistorikkAvklaring, tiltakshistorikkOppfolging),
            meldinger = setOf(),
        )

        val deltakelseOppfolgingUtenStartdato = deltakelseOppfolgingFraKomet.copy(
            deltakerId = UUID.randomUUID(),
            status = DeltakelseFraKomet.Status(type = DeltakerStatus.Type.KLADD, visningstekst = "Kladd"),
            periode = null,
        )

        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(deltakelseOppfolgingFraKomet, deltakelseOppfolgingUtenStartdato),
                historikk = emptyList(),
            ),
        )

        val historikkService = createTiltakshistorikkService()

        val historikk = historikkService.hentHistorikk(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        )

        historikk shouldBe Deltakelser(
            meldinger = setOf(),
            aktive = listOf(
                deltakelseOppfolgingUtenStartdato.toDeltakelse(),
                deltakelseOppfolging,
                deltakelseAvklaring,
            ),
            historiske = emptyList(),
        )
    }
})
