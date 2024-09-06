package no.nav.mulighetsrommet.api.services

import arrow.core.Either
import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.*
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.DeltakerKort
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk.Arrangor
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk.Gjennomforing
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkResponse
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltaksgjennomforing = TiltaksgjennomforingFixtures.Oppfolging1

    val deltakelseOppfolging = Tiltakshistorikk.GruppetiltakDeltakelse(
        id = UUID.randomUUID(),
        gjennomforing = Gjennomforing(
            id = tiltaksgjennomforing.id,
            navn = tiltaksgjennomforing.navn,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode!!,
        ),
        norskIdent = NorskIdent("12345678910"),
        status = AmtDeltakerStatus(
            type = AmtDeltakerStatus.Type.VENTELISTE,
            opprettetDato = LocalDateTime.of(2018, 12, 3, 0, 0),
            aarsak = null,
        ),
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arrangor = Arrangor(Organisasjonsnummer(ArrangorFixtures.underenhet1.organisasjonsnummer)),
    )

    val deltakelseAvklaring = Tiltakshistorikk.ArenaDeltakelse(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.VENTELISTE,
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arenaTiltakskode = TiltakstypeFixtures.Avklaring.arenaKode,
        beskrivelse = "Avklaring",
        arrangor = Arrangor(Organisasjonsnummer("123456789")),
    )

    val deltakelseOppfolgingFraKomet = DeltakelseFraKomet(
        deltakerId = deltakelseOppfolging.id,
        deltakerlisteId = deltakelseOppfolging.gjennomforing.id,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstype = DeltakelserResponse.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            tiltakskode = GruppeTiltakstype.INDOPPFAG,
        ),
        status = DeltakerStatus(
            type = DeltakerStatus.DeltakerStatusType.VENTELISTE,
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

    val deltakerKortOppfolging = DeltakerKort(
        id = deltakelseOppfolging.id,
        tiltaksgjennomforingId = deltakelseOppfolging.gjennomforing.id,
        eierskap = DeltakerKort.Eierskap.KOMET,
        tittel = "Oppfølging hos Fretex AS",
        tiltakstypeNavn = TiltakstypeFixtures.Oppfolging.navn,
        status = DeltakerKort.DeltakerStatus(
            type = DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTELISTE,
            visningstekst = "Venteliste",
            aarsak = null,
        ),
        periode = DeltakerKort.Periode(
            startdato = LocalDate.of(2019, 1, 1),
            sluttdato = LocalDate.of(2019, 12, 3),
        ),
        sistEndretDato = LocalDate.of(2018, 12, 5),
        innsoktDato = LocalDate.of(2018, 12, 3),
    )
    val deltakerKortAvklaring = DeltakerKort(
        id = deltakelseAvklaring.id,
        tiltaksgjennomforingId = null,
        eierskap = DeltakerKort.Eierskap.ARENA,
        tittel = "Avklaring",
        tiltakstypeNavn = TiltakstypeFixtures.Avklaring.navn,
        status = DeltakerKort.DeltakerStatus(
            type = DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTELISTE,
            visningstekst = "Venteliste",
            aarsak = null,
        ),
        periode = DeltakerKort.Periode(
            startdato = LocalDate.of(2018, 12, 3),
            sluttdato = LocalDate.of(2019, 12, 3),
        ),
        sistEndretDato = null,
        innsoktDato = null,
    )

    val pdlClient: PdlClient = mockk()
    val tiltakshistorikkClient: TiltakshistorikkClient = mockk()
    val amtDeltakerClient: AmtDeltakerClient = mockk()

    fun createTiltakshistorikkService() = TiltakshistorikkService(
        pdlClient = pdlClient,
        amtDeltakerClient = amtDeltakerClient,
        tiltakshistorikkClient = tiltakshistorikkClient,
        arrangorService = ArrangorService(mockk(), ArrangorRepository(database.db)),
        tiltakstypeRepository = TiltakstypeRepository(database.db),
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
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging, TiltakstypeFixtures.Avklaring),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(tiltaksgjennomforing),
        ).initialize(database.db)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(deltakelseOppfolging, deltakelseAvklaring),
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
            aktive = listOf(deltakerKortOppfolging, deltakerKortAvklaring),
            historiske = emptyList(),
        )
    }

    test("inkluderer deltakelser fra komet når de ikke finnes i tiltakshistorikken") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(deltakelseAvklaring),
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
            aktive = listOf(deltakerKortOppfolging, deltakerKortAvklaring),
            historiske = emptyList(),
        )
    }

    test("viser kun deltakelser fra tiltakshistorikken når det ikke returneres deltakelser fra komet") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(deltakelseAvklaring),
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
            aktive = listOf(deltakerKortAvklaring),
            historiske = emptyList(),
        )
    }

    test("sorterer deltakelser basert nyeste startdato") {
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(deltakelseAvklaring, deltakelseOppfolging),
        )

        val deltakelseOppfolgingUtenStartdato = deltakelseOppfolgingFraKomet.copy(
            deltakerId = UUID.randomUUID(),
            status = DeltakerStatus(type = DeltakerStatus.DeltakerStatusType.KLADD, visningstekst = "Kladd"),
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
            aktive = listOf(
                deltakelseOppfolgingUtenStartdato.toDeltakerKort(),
                deltakerKortOppfolging,
                deltakerKortAvklaring,
            ),
            historiske = emptyList(),
        )
    }
})
