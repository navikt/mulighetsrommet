package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.clients.teamtiltak.TeamTiltakClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakshistorikkRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakshistorikkServiceTest : FunSpec({
    val arrangorService: ArrangorService = mockk()
    val teamTiltakClient: TeamTiltakClient = mockk()
    coEvery { teamTiltakClient.getAvtaler(any()) } returns emptyList()

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val tiltakstype = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakskode = "ARBTREN",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )

    val tiltaksgjennomforing = TiltaksgjennomforingDbo(
        id = UUID.randomUUID(),
        navn = "Arbeidstrening",
        tiltakstypeId = tiltakstype.id,
        tiltaksnummer = "12345",
        virksomhetsnummer = "123456789",
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        startDato = LocalDate.of(2022, 1, 1),
        tilgjengelighet = TiltaksgjennomforingDbo.Tilgjengelighetsstatus.LEDIG,
        antallPlasser = null,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingDbo.Oppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.ARENA,
    )

    val tiltakshistorikkGruppe = TiltakshistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
    )

    val tiltakstypeIndividuell = TiltakstypeDbo(
        id = UUID.randomUUID(),
        navn = "Høyere utdanning",
        tiltakskode = "HOYEREUTD",
        rettPaaTiltakspenger = true,
        registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
        fraDato = LocalDate.of(2023, 1, 11),
        tilDato = LocalDate.of(2023, 1, 12),
    )

    val tiltakshistorikkIndividuell = TiltakshistorikkDbo.IndividueltTiltak(
        id = UUID.randomUUID(),
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        beskrivelse = "Utdanning",
        tiltakstypeId = tiltakstypeIndividuell.id,
        virksomhetsnummer = "12343",
    )

    beforeSpec {
        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstype)
        tiltakstyper.upsert(tiltakstypeIndividuell)

        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        tiltaksgjennomforinger.upsert(tiltaksgjennomforing)

        val tiltakshistorikk = TiltakshistorikkRepository(database.db)
        tiltakshistorikk.upsert(tiltakshistorikkGruppe)
        tiltakshistorikk.upsert(tiltakshistorikkIndividuell)
    }

    test("henter historikk for bruker basert på person id med arrangørnavn") {
        val bedriftsnavn = "Bedriftsnavn"
        val bedriftsnavn2 = "Bedriftsnavn 2"
        coEvery { arrangorService.hentOverordnetEnhetNavnForArrangor("123456789") } returns bedriftsnavn
        coEvery { arrangorService.hentOverordnetEnhetNavnForArrangor("12343") } returns bedriftsnavn2

        val historikkService = TiltakshistorikkService(
            arrangorService,
            TiltakshistorikkRepository(database.db),
            teamTiltakClient,
        )

        val forventetHistorikk = listOf(
            TiltakshistorikkDto(
                id = tiltakshistorikkGruppe.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Arbeidstrening",
                tiltakstype = "Arbeidstrening",
                arrangor = TiltakshistorikkDto.Arrangor(virksomhetsnummer = "123456789", navn = bedriftsnavn),
            ),
            TiltakshistorikkDto(
                id = tiltakshistorikkIndividuell.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Utdanning",
                tiltakstype = "Høyere utdanning",
                arrangor = TiltakshistorikkDto.Arrangor(virksomhetsnummer = "12343", navn = bedriftsnavn2),
            ),
        )

        historikkService.hentHistorikkForBruker("12345678910") shouldBe forventetHistorikk
    }
})
