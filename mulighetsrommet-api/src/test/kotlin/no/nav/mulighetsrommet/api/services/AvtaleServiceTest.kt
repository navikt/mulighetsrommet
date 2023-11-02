package no.nav.mulighetsrommet.api.services

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.api.avtaler.AvtaleValidator
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.NavAnsattRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.util.*

class AvtaleServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val virksomhetService: VirksomhetService = mockk(relaxed = true)
    val notificationRepository: NotificationRepository = mockk(relaxed = true)
    val utkastRepository: UtkastRepository = mockk(relaxed = true)
    val validator = mockk<AvtaleValidator>()

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        database.db.truncateAll()
        domain.initialize(database.db)

        every { validator.validate(any()) } answers {
            firstArg<AvtaleDbo>().right()
        }
    }

    context("Upsert avtale") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            notificationRepository,
            utkastRepository,
            validator,
            database.db,
        )

        test("Man skal ikke få lov til å opprette avtale dersom det oppstår valideringsfeil") {
            val request = AvtaleFixtures.avtaleRequest

            every { validator.validate(request.toDbo()) } returns listOf(ValidationError("navn", "Dårlig navn")).left()

            avtaleService.upsert(request, "B123456").shouldBeLeft(
                listOf(ValidationError("navn", "Dårlig navn")),
            )
        }
    }

    context("Slette avtale") {
        val avtaler = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            notificationRepository,
            utkastRepository,
            validator,
            database.db,
        )

        test("Man skal ikke få slette dersom avtalen ikke finnes") {
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()

            avtaleService.delete(avtaleIdSomIkkeFinnes).shouldBeLeft(
                NotFound(message = "Avtalen finnes ikke"),
            )
        }

        test("Man skal ikke få slette, men få en melding dersom dagens dato er mellom start- og sluttdato for avtalen") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
            )
            avtaler.upsert(avtale)

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeLeft(
                BadRequest("Avtalen er aktiv og kan derfor ikke slettes."),
            )
        }

        test("Man skal ikke få slette, men få en melding dersom opphav for avtalen ikke er admin-flate") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            avtaler.upsert(avtale)

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeLeft(
                BadRequest("Avtalen har opprinnelse fra Arena og kan ikke bli slettet i admin-flate."),
            )
        }

        test("Man skal ikke få slette, men få en melding dersom det finnes gjennomføringer koblet til avtalen") {
            val currentDate = LocalDate.of(2023, 6, 1)
            val avtale = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            val avtaleSomErUinteressant = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som vi ikke bryr oss om",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            avtaler.upsert(avtale)
            avtaler.upsert(avtaleSomErUinteressant)
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

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeLeft(
                BadRequest("Avtalen har 2 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan slette avtalen."),
            )
        }

        test("Skal få slette avtale hvis alle sjekkene er ok") {
            val currentDate = LocalDate.of(2023, 6, 1)

            val avtale = AvtaleFixtures.avtale1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2024, 7, 1),
            )
            avtaler.upsert(avtale).right()

            avtaleService.delete(avtale.id, currentDate = currentDate).shouldBeRight()
        }
    }

    context("Avbryte avtale") {
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        val avtaleService = AvtaleService(
            avtaleRepository,
            tiltaksgjennomforinger,
            virksomhetService,
            notificationRepository,
            utkastRepository,
            validator,
            database.db,
        )

        test("Man skal ikke få avbryte dersom avtalen ikke finnes") {
            val avtale = AvtaleFixtures.avtale1.copy(navn = "Avtale som eksisterer")
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()
            avtaleRepository.upsert(avtale)

            avtaleService.avbrytAvtale(avtaleIdSomIkkeFinnes).shouldBeLeft(
                NotFound("Avtalen finnes ikke"),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom opphav for avtalen ikke er admin-flate") {
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 6, 1),
                sluttDato = LocalDate.of(2023, 7, 1),
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            avtaleRepository.upsert(avtale)

            avtaleService.avbrytAvtale(avtale.id).shouldBeLeft(
                BadRequest("Avtalen har opprinnelse fra Arena og kan ikke bli avbrutt fra admin-flate."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val avtale = AvtaleFixtures.avtale1.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = LocalDate.of(2023, 6, 1),
                opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
            )
            avtaleRepository.upsert(avtale)

            avtaleService.avbrytAvtale(avtale.id).shouldBeLeft(
                BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.avtale1.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            val avtaleSomErUinteressant = AvtaleFixtures.avtale1.copy(
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

            avtaleService.avbrytAvtale(avtale.id).shouldBeLeft(
                BadRequest("Avtalen har 2 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan avbryte avtalen."),
            )
        }

        test("Skal få avbryte avtale hvis alle sjekkene er ok") {
            val avtale = AvtaleFixtures.avtale1.copy(
                startDato = LocalDate.of(2023, 7, 1),
                sluttDato = LocalDate.of(2024, 7, 1),
            )
            avtaleRepository.upsert(avtale).right()

            avtaleService.avbrytAvtale(avtale.id)
        }
    }

    context("Administrator-notification") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            notificationRepository,
            utkastRepository,
            validator,
            database.db,
        )
        val navAnsattRepository = NavAnsattRepository(database.db)

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
                    fornavn = "Bertil",
                    etternavn = "Betabruker",
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

            verify(exactly = 0) { notificationRepository.insert(any(), any()) }
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = "B123456",
                    fornavn = "Bertil",
                    etternavn = "Betabruker",
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
                    etternavn = "Betabruker",
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
            avtaleService.upsert(avtale.copy(navn = "nytt navn", administratorer = listOf("Z654321", "T654321", "B123456")), "B123456")

            verify(exactly = 2) { notificationRepository.insert(any(), any()) }
        }
    }

    context("transactions") {
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            virksomhetService,
            notificationRepository,
            utkastRepository,
            validator,
            database.db,
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
