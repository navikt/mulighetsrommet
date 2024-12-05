package no.nav.mulighetsrommet.api.refusjon.task

import arrow.core.right
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.clients.dokark.DokarkClient
import no.nav.mulighetsrommet.api.clients.dokark.DokarkResponse
import no.nav.mulighetsrommet.api.clients.pdl.PdlClient
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.*
import no.nav.mulighetsrommet.api.refusjon.HentAdressebeskyttetPersonBolkPdlQuery
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningAft
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravPeriode
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.Kontonummer
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class JournalforRefusjonskravTest : FunSpec({
    val databaseConfig = databaseConfig
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val hovedenhet = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("883674471"),
        navn = "Hovedenhet",
        postnummer = "0102",
        poststed = "Oslo",
    )
    val barnevernsNembda = ArrangorDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = Organisasjonsnummer("973674471"),
        navn = "BARNEVERNS- OG HELSENEMNDA I BUSKERUD OG OMEGN",
        postnummer = "0102",
        poststed = "Oslo",
        overordnetEnhet = hovedenhet.organisasjonsnummer,
    )
    val krav = RefusjonskravDbo(
        id = UUID.randomUUID(),
        gjennomforingId = TiltaksgjennomforingFixtures.AFT1.id,
        fristForGodkjenning = LocalDateTime.now(),
        beregning = RefusjonKravBeregningAft(
            input = RefusjonKravBeregningAft.Input(
                periode = RefusjonskravPeriode.fromDayInMonth(LocalDate.of(2024, 8, 1)),
                sats = 20205,
                deltakelser = emptySet(),
            ),
            output = RefusjonKravBeregningAft.Output(
                belop = 0,
                deltakelser = emptySet(),
            ),
        ),
        kontonummer = Kontonummer("12312312312"),
        kid = null,
    )

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
        ansatte = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
        avtaler = listOf(
            AvtaleFixtures.AFT.copy(
                arrangorId = hovedenhet.id,
                arrangorUnderenheter = listOf(barnevernsNembda.id),
            ),
        ),
        gjennomforinger = listOf(TiltaksgjennomforingFixtures.AFT1.copy(arrangorId = barnevernsNembda.id)),
        deltakere = emptyList(),
        arrangorer = listOf(hovedenhet, barnevernsNembda),
        refusjonskrav = listOf(krav),
    )

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)
    }

    val pdl: PdlClient = mockk(relaxed = true)
    val tilsagnService: TilsagnService = mockk()
    val dokarkClient: DokarkClient = mockk()
    val hentAdressebeskyttetPersonBolkPdlQuery = HentAdressebeskyttetPersonBolkPdlQuery(pdl)

    test("krav må være godkjent") {
        val task = JournalforRefusjonskrav(
            database.db,
            refusjonskravRepository = RefusjonskravRepository(database.db),
            tilsagnService,
            dokarkClient,
            deltakerRepository = DeltakerRepository(database.db),
            hentAdressebeskyttetPersonBolkPdlQuery,
        )

        shouldThrow<Throwable> {
            task.journalforRefusjonskrav(krav.id)
        }
    }

    test("vellykket journalføring setter journalpost_id") {
        val refusjonskravRepository = RefusjonskravRepository(database.db)
        val task = JournalforRefusjonskrav(
            database.db,
            refusjonskravRepository,
            tilsagnService,
            dokarkClient,
            deltakerRepository = DeltakerRepository(database.db),
            hentAdressebeskyttetPersonBolkPdlQuery,
        )
        refusjonskravRepository.setGodkjentAvArrangor(krav.id, LocalDateTime.now())

        every { tilsagnService.getArrangorflateTilsagnTilRefusjon(any(), any()) } returns emptyList()
        coEvery { dokarkClient.opprettJournalpost(any(), any()) } returns DokarkResponse(
            journalpostId = "123",
            journalstatus = "ok",
            melding = null,
            journalpostferdigstilt = true,
            dokumenter = emptyList(),
        ).right()

        task.journalforRefusjonskrav(krav.id)
        refusjonskravRepository.get(krav.id)?.journalpostId shouldBe "123"
    }
})
