package no.nav.mulighetsrommet.api.services

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.util.*

class AvtaleServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val utkastRepository: UtkastRepository = mockk(relaxed = true)
    val enabledTiltakstyper = listOf(TiltakstypeFixtures.Oppfolging.tiltakskode)
    val validator = mockk<AvtaleValidator>()

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<AvtaleDbo>().right()
        }

        coEvery { virksomhetService.getOrSyncVirksomhetFromBrreg(any()) } answers {
            VirksomhetDto(
                organisasjonsnummer = firstArg<String>(),
                navn = "Virksomhet",
                postnummer = null,
                poststed = null,
            ).right()
        }
    }

    context("Upsert avtale") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            NotificationRepository(database.db),
            utkastRepository,
            tiltakstypeRepository,
            validator,
            EndringshistorikkService(database.db),
            database.db,
            enabledTiltakstyper,
        )

        test("Man skal ikke få lov til å opprette avtale dersom det oppstår valideringsfeil") {
            val request = AvtaleFixtures.avtaleRequest

            every { validator.validate(request.toDbo(), any()) } returns listOf(
                ValidationError("navn", "Dårlig navn"),
            ).left()

            avtaleService.upsert(request, "B123456").shouldBeLeft(
                listOf(ValidationError("navn", "Dårlig navn")),
            )
        }
    }

    context("Avbryte avtale") {
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val tiltakstypeRepository = TiltakstypeRepository(database.db)

        val avtaleService = AvtaleService(
            avtaleRepository,
            tiltaksgjennomforinger,
            virksomhetService,
            NotificationRepository(database.db),
            utkastRepository,
            tiltakstypeRepository,
            validator,
            EndringshistorikkService(database.db),
            database.db,
            enabledTiltakstyper,
        )

        test("Man skal ikke få avbryte dersom avtalen ikke finnes") {
            val avtale = AvtaleFixtures.oppfolging.copy(navn = "Avtale som eksisterer")
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()
            avtaleRepository.upsert(avtale)

            avtaleService.avbrytAvtale(avtaleIdSomIkkeFinnes, "B123456").shouldBeLeft(
                NotFound("Avtalen finnes ikke"),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom opphav for avtalen ikke er admin-flate") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )
            avtaleRepository.upsert(avtale)
            avtaleRepository.setOpphav(avtale.id, ArenaMigrering.Opphav.ARENA)

            avtaleService.avbrytAvtale(avtale.id, "B123456").shouldBeLeft(
                BadRequest("Avtalen har opprinnelse fra Arena og kan ikke bli avbrutt fra admin-flate."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = LocalDate.of(2023, 6, 1),
            )
            avtaleRepository.upsert(avtale)
            avtaleRepository.setOpphav(avtale.id, ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            avtaleService.avbrytAvtale(avtale.id, "B123456").shouldBeLeft(
                BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            val avtaleSomErUinteressant = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som vi ikke bryr oss om",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            avtaleRepository.upsert(avtale)
            avtaleRepository.upsert(avtaleSomErUinteressant)
            val oppfolging = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val arbeidstrening = TiltaksgjennomforingFixtures.Arbeidstrening1.copy(
                avtaleId = avtale.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val oppfolging2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtaleSomErUinteressant.id,
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            tiltaksgjennomforinger.upsert(oppfolging)
            tiltaksgjennomforinger.upsert(arbeidstrening)
            tiltaksgjennomforinger.upsert(oppfolging2)

            avtaleService.avbrytAvtale(avtale.id, "B123456").shouldBeLeft(
                BadRequest("Avtalen har 2 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan avbryte avtalen."),
            )
        }

        test("Skal få avbryte avtale hvis alle sjekkene er ok") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2024, 7, 1),
            )
            avtaleRepository.upsert(avtale).right()

            avtaleService.avbrytAvtale(avtale.id, "B123456")
        }
    }

    context("Administrator-notification") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            NotificationRepository(database.db),
            utkastRepository,
            tiltakstypeRepository,
            validator,
            EndringshistorikkService(database.db),
            database.db,
            enabledTiltakstyper,
        )
        val navAnsattRepository = NavAnsattRepository(database.db)

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
                    fornavn = "Bertil",
                    etternavn = "Bengtson",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf("B123456"))
            avtaleService.upsert(avtale, "B123456")

            database.assertThat("user_notification").isEmpty
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
                    fornavn = "Bertil",
                    etternavn = "Bengtson",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "Z654321",
                    fornavn = "Zorre",
                    etternavn = "Zorreszon",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "T654321",
                    fornavn = "Tuva",
                    etternavn = "Testpilot",
                    hovedenhet = "2990",
                    azureId = UUID.randomUUID(),
                    mobilnummer = null,
                    epost = "",
                    roller = emptySet(),
                    skalSlettesDato = null,
                ),
            )
            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf("Z654321"))
            avtaleService.upsert(avtale, "B123456")

            database.assertThat("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues("Z654321")

            avtaleService.upsert(
                avtale.copy(
                    navn = "nytt navn",
                    administratorer = listOf("Z654321", "T654321", "B123456"),
                ),
                "B123456",
            )

            database.assertThat("user_notification")
                .hasNumberOfRows(2)
                .column("user_id")
                .containsValues("Z654321", "T654321")
        }
    }

    context("transactions") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            NotificationRepository(database.db),
            utkastRepository,
            tiltakstypeRepository,
            validator,
            EndringshistorikkService(database.db),
            database.db,
            enabledTiltakstyper,
        )

        test("Hvis is utkast _ikke_ kaster blir upsert værende") {
            val avtale = AvtaleFixtures.avtaleRequest.copy(id = UUID.randomUUID())

            avtaleService.upsert(avtale, "Z123456")

            avtaleService.get(avtale.id) shouldNotBe null
        }

        test("Hvis utkast kaster rulles upsert tilbake") {
            val avtale = AvtaleFixtures.avtaleRequest.copy(id = UUID.randomUUID())

            every { utkastRepository.delete(any(), any()) } throws Exception()

            shouldThrow<Throwable> { avtaleService.upsert(avtale, "B123456") }

            avtaleService.get(avtale.id) shouldBe null
        }
    }
})
