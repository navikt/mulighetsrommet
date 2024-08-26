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
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.DeltakerKort
import no.nav.mulighetsrommet.api.fixtures.*
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

    val arrangorService: ArrangorService = mockk()
    val pdlClient: PdlClient = mockk()
    val tiltakshistorikkClient: TiltakshistorikkClient = mockk()
    val amtDeltakerClient: AmtDeltakerClient = mockk()
    val tiltakstype = TiltakstypeFixtures.Oppfolging

    val tiltaksgjennomforing = TiltaksgjennomforingFixtures.Oppfolging1

    val gruppetiltakDeltakelse = Tiltakshistorikk.GruppetiltakDeltakelse(
        id = UUID.randomUUID(),
        gjennomforing = Gjennomforing(
            id = tiltaksgjennomforing.id,
            navn = tiltaksgjennomforing.navn,
            tiltakskode = tiltakstype.tiltakskode!!,
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

    val tiltakstypeGruppe = TiltakstypeFixtures.Avklaring

    val arenaDeltakelse = Tiltakshistorikk.ArenaDeltakelse(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.VENTELISTE,
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arenaTiltakskode = tiltakstypeGruppe.arenaKode,
        beskrivelse = "Utdanning",
        arrangor = Arrangor(Organisasjonsnummer("123456789")),
    )

    beforeAny {
        MulighetsrommetTestDomain(
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            tiltakstyper = listOf(tiltakstype, tiltakstypeGruppe),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(tiltaksgjennomforing),
        ).initialize(database.db)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        coEvery { arrangorService.getOrSyncArrangorFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer) } returns ArrangorFixtures.underenhet1.right()
        coEvery { arrangorService.getOrSyncArrangorFromBrreg(arenaDeltakelse.arrangor.organisasjonsnummer.value) } returns ArrangorDto(
            id = UUID.randomUUID(),
            navn = "Bedriftsnavn 2",
            organisasjonsnummer = arenaDeltakelse.arrangor.organisasjonsnummer.value,
            postnummer = null,
            poststed = null,
        ).right()
        coEvery { pdlClient.hentHistoriskeIdenter(any(), any()) } returns listOf(
            IdentInformasjon(
                ident = PdlIdent("12345678910"),
                gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                historisk = false,
            ),
        ).right()
        coEvery { tiltakshistorikkClient.historikk(any()) } returns TiltakshistorikkResponse(
            historikk = listOf(gruppetiltakDeltakelse, arenaDeltakelse),
        )
        coEvery { amtDeltakerClient.hentDeltakelser(any(), any()) } returns Either.Right(
            DeltakelserResponse(
                aktive = listOf(
                    DeltakelseFraKomet(
                        deltakerId = gruppetiltakDeltakelse.id,
                        deltakerlisteId = gruppetiltakDeltakelse.gjennomforing.id,
                        tittel = "Oppfølging hos Fretex AS",
                        tiltakstype = DeltakelserResponse.Tiltakstype(navn = tiltakstype.navn, tiltakskode = GruppeTiltakstype.INDOPPFAG),
                        status = DeltakerStatus(
                            type = DeltakerStatus.DeltakerStatusType.VENTELISTE,
                            visningstekst = "Venteliste",
                            aarsak = null,
                        ),
                        periode = Periode(
                            startdato = LocalDate.of(2018, 12, 3),
                            sluttdato = LocalDate.of(2019, 12, 3),
                        ),
                        innsoktDato = LocalDate.of(2018, 12, 3),
                        sistEndretDato = LocalDate.of(2018, 12, 5),
                    ),
                ),
                historikk = emptyList(),
            ),
        )

        val tiltakstyper = TiltakstypeRepository(database.db)
        val historikkService = TiltakshistorikkService(
            pdlClient,
            arrangorService,
            amtDeltakerClient,
            tiltakshistorikkClient,
            tiltakstyper,
        )

        val forventetHistorikk: Map<String, List<DeltakerKort>> = mapOf(
            "aktive" to listOf(
                DeltakerKort(
                    id = gruppetiltakDeltakelse.id,
                    tiltaksgjennomforingId = null,
                    eierskap = DeltakerKort.Eierskap.KOMET,
                    tittel = "Oppfølging hos Fretex AS",
                    tiltakstypeNavn = tiltakstype.navn,
                    status = DeltakerKort.DeltakerStatus(
                        type = DeltakerKort.DeltakerStatus.DeltakerStatusType.VENTELISTE,
                        visningstekst = "Venteliste",
                        aarsak = null,

                    ),
                    periode = DeltakerKort.Periode(
                        startdato = LocalDate.of(2018, 12, 3),
                        sluttdato = LocalDate.of(2019, 12, 3),
                    ),
                    sistEndretDato = LocalDate.of(2018, 12, 5),
                    innsoktDato = LocalDate.of(2018, 12, 3),
                ),
                DeltakerKort(
                    id = arenaDeltakelse.id,
                    tiltaksgjennomforingId = null,
                    eierskap = DeltakerKort.Eierskap.ARENA,
                    tittel = "Utdanning",
                    tiltakstypeNavn = tiltakstypeGruppe.navn,
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
                ),
            ),
            "historiske" to emptyList(),
        )

        historikkService.hentHistorikkForBruker(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        ) shouldBe forventetHistorikk
    }
})
