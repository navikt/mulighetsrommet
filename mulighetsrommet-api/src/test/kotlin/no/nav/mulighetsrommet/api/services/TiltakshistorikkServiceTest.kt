package no.nav.mulighetsrommet.api.services

import arrow.core.right
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.IdentInformasjon
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.tiltakshistorikk.TiltakshistorikkClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkAdminDto
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

    val tiltakstypeIndividuell = TiltakstypeFixtures.Arbeidstrening

    val arenaDeltakelse = Tiltakshistorikk.ArenaDeltakelse(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        status = ArenaDeltakerStatus.VENTELISTE,
        startDato = LocalDate.of(2018, 12, 3),
        sluttDato = LocalDate.of(2019, 12, 3),
        arenaTiltakskode = tiltakstypeIndividuell.arenaKode,
        beskrivelse = "Utdanning",
        arrangor = Arrangor(Organisasjonsnummer("123456789")),
    )

    beforeAny {
        MulighetsrommetTestDomain(
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            tiltakstyper = listOf(tiltakstype, tiltakstypeIndividuell),
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

        val tiltakstyper = TiltakstypeRepository(database.db)
        val historikkService = TiltakshistorikkService(
            pdlClient,
            arrangorService,
            amtDeltakerClient,
            tiltakshistorikkClient,
            tiltakstyper,
        )

        val forventetHistorikk = listOf(
            TiltakshistorikkAdminDto.GruppetiltakDeltakelse(
                id = gruppetiltakDeltakelse.id,
                startDato = LocalDate.of(2018, 12, 3),
                sluttDato = LocalDate.of(2019, 12, 3),
                status = AmtDeltakerStatus(
                    type = AmtDeltakerStatus.Type.VENTELISTE,
                    opprettetDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                    aarsak = null,
                ),
                tiltakNavn = tiltaksgjennomforing.navn,
                tiltakstypeNavn = tiltakstype.navn,
                arrangor = TiltakshistorikkAdminDto.Arrangor(
                    organisasjonsnummer = Organisasjonsnummer(ArrangorFixtures.underenhet1.organisasjonsnummer),
                    navn = ArrangorFixtures.underenhet1.navn,
                ),
            ),
            TiltakshistorikkAdminDto.ArenaDeltakelse(
                id = arenaDeltakelse.id,
                startDato = LocalDate.of(2018, 12, 3),
                sluttDato = LocalDate.of(2019, 12, 3),
                status = ArenaDeltakerStatus.VENTELISTE,
                tiltakNavn = arenaDeltakelse.beskrivelse,
                tiltakstypeNavn = tiltakstypeIndividuell.navn,
                arrangor = TiltakshistorikkAdminDto.Arrangor(
                    organisasjonsnummer = arenaDeltakelse.arrangor.organisasjonsnummer,
                    navn = "Bedriftsnavn 2",
                ),
            ),
        )

        historikkService.hentHistorikkForBruker(
            NorskIdent("12345678910"),
            AccessType.OBO("token"),
        ) shouldBe forventetHistorikk
    }
})
