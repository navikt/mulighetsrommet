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
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltakshistorikkDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
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

    val tiltakshistorikkGruppe = ArenaTiltakshistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = NorskIdent("12345678910"),
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
    )

    val tiltakstypeIndividuell = TiltakstypeFixtures.Arbeidstrening

    val tiltakshistorikkIndividuell = ArenaTiltakshistorikkDbo.IndividueltTiltak(
        id = UUID.randomUUID(),
        norskIdent = NorskIdent("12345678910"),
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        beskrivelse = "Utdanning",
        tiltakstypeId = tiltakstypeIndividuell.id,
        arrangorOrganisasjonsnummer = "123456789",
    )

    beforeAny {
        MulighetsrommetTestDomain(
            arrangorer = listOf(ArrangorFixtures.hovedenhet, ArrangorFixtures.underenhet1),
            tiltakstyper = listOf(tiltakstype, tiltakstypeIndividuell),
            avtaler = listOf(AvtaleFixtures.oppfolging),
            gjennomforinger = listOf(tiltaksgjennomforing),
        ).initialize(database.db)

        val tiltakshistorikk = TiltakshistorikkRepository(database.db)
        tiltakshistorikk.upsert(tiltakshistorikkGruppe)
        tiltakshistorikk.upsert(tiltakshistorikkIndividuell)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        coEvery { arrangorService.getOrSyncArrangorFromBrreg(ArrangorFixtures.underenhet1.organisasjonsnummer) } returns ArrangorFixtures.underenhet1.right()
        coEvery { arrangorService.getOrSyncArrangorFromBrreg(tiltakshistorikkIndividuell.arrangorOrganisasjonsnummer) } returns ArrangorDto(
            id = UUID.randomUUID(),
            navn = "Bedriftsnavn 2",
            organisasjonsnummer = tiltakshistorikkIndividuell.arrangorOrganisasjonsnummer,
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

        val tiltakshistorikk = TiltakshistorikkRepository(database.db)
        val tiltakstyper = TiltakstypeRepository(database.db)
        val historikkService = TiltakshistorikkService(
            pdlClient,
            arrangorService,
            amtDeltakerClient,
            tiltakshistorikk,
            tiltakshistorikkClient,
            tiltakstyper,
        )

        val forventetHistorikk = listOf(
            TiltakshistorikkAdminDto(
                id = tiltakshistorikkGruppe.id,
                fraDato = LocalDate.of(2018, 12, 3),
                tilDato = LocalDate.of(2019, 12, 3),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = tiltaksgjennomforing.navn,
                tiltakstype = tiltakstype.navn,
                arrangor = TiltakshistorikkAdminDto.Arrangor(
                    organisasjonsnummer = Organisasjonsnummer(ArrangorFixtures.underenhet1.organisasjonsnummer),
                    navn = ArrangorFixtures.underenhet1.navn,
                ),
            ),
            TiltakshistorikkAdminDto(
                id = tiltakshistorikkIndividuell.id,
                fraDato = LocalDate.of(2018, 12, 3),
                tilDato = LocalDate.of(2019, 12, 3),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = tiltakshistorikkIndividuell.beskrivelse,
                tiltakstype = tiltakstypeIndividuell.navn,
                arrangor = TiltakshistorikkAdminDto.Arrangor(
                    organisasjonsnummer = Organisasjonsnummer(tiltakshistorikkIndividuell.arrangorOrganisasjonsnummer),
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
