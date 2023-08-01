package no.nav.mulighetsrommet.api.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures.avtale1
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures.avtale1Id
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
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
        arrangorOrganisasjonsnummer = "123456789",
        arenaAnsvarligEnhet = "2990",
        avslutningsstatus = Avslutningsstatus.AVSLUTTET,
        startDato = LocalDate.of(2022, 1, 1),
        tilgjengelighet = TiltaksgjennomforingTilgjengelighetsstatus.LEDIG,
        antallPlasser = 12,
        ansvarlige = emptyList(),
        navEnheter = emptyList(),
        oppstart = TiltaksgjennomforingOppstartstype.FELLES,
        opphav = ArenaMigrering.Opphav.ARENA,
        sluttDato = null,
        kontaktpersoner = emptyList(),
        arrangorKontaktpersonId = null,
        stengtFra = null,
        stengtTil = null,
        lokasjonArrangor = "Oslo",
        estimertVentetid = null,
        avtaleId = avtale1Id,
    )

    val tiltakshistorikkGruppe = ArenaTiltakshistorikkDbo.Gruppetiltak(
        id = UUID.randomUUID(),
        tiltaksgjennomforingId = tiltaksgjennomforing.id,
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
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

    val tiltakshistorikkIndividuell = ArenaTiltakshistorikkDbo.IndividueltTiltak(
        id = UUID.randomUUID(),
        norskIdent = "12345678910",
        status = Deltakerstatus.VENTER,
        fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
        registrertIArenaDato = LocalDateTime.of(2018, 12, 3, 0, 0),
        beskrivelse = "Utdanning",
        tiltakstypeId = tiltakstypeIndividuell.id,
        arrangorOrganisasjonsnummer = "12343",
    )

    beforeSpec {
        val tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(tiltakstype)
        tiltakstyper.upsert(tiltakstypeIndividuell)
        val avtaler = AvtaleRepository(database.db)
        avtaler.upsert(avtale1)

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

        val historikkService = TiltakshistorikkService(arrangorService, TiltakshistorikkRepository(database.db))

        val forventetHistorikk = listOf(
            TiltakshistorikkDto(
                id = tiltakshistorikkGruppe.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Arbeidstrening",
                tiltakstype = "Arbeidstrening",
                arrangor = TiltakshistorikkDto.Arrangor(organisasjonsnummer = "123456789", navn = bedriftsnavn),
            ),
            TiltakshistorikkDto(
                id = tiltakshistorikkIndividuell.id,
                fraDato = LocalDateTime.of(2018, 12, 3, 0, 0),
                tilDato = LocalDateTime.of(2019, 12, 3, 0, 0),
                status = Deltakerstatus.VENTER,
                tiltaksnavn = "Utdanning",
                tiltakstype = "Høyere utdanning",
                arrangor = TiltakshistorikkDto.Arrangor(organisasjonsnummer = "12343", navn = bedriftsnavn2),
            ),
        )

        historikkService.hentHistorikkForBruker("12345678910") shouldBe forventetHistorikk
    }
})
