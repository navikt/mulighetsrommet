package no.nav.mulighetsrommet.api.avtale

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.clients.brreg.BrregClient
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRepository
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.NotFound
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.AvbruttAarsak
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.notifications.NotificationRepository
import no.nav.mulighetsrommet.utils.toUUID
import java.time.LocalDate
import java.util.*

class AvtaleServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val validator = mockk<AvtaleValidator>()

    val domain = MulighetsrommetTestDomain()

    beforeEach {
        domain.initialize(database.db)

        every { validator.validate(any(), any()) } answers {
            firstArg<AvtaleDbo>().right()
        }
    }

    afterEach {
        database.db.truncateAll()
    }

    val bertilNavIdent = NavIdent("B123456")

    context("Upsert avtale") {
        val brregClient = mockk<BrregClient>()
        val arrangorService = ArrangorService(brregClient, ArrangorRepository(database.db))
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            arrangorService,
            NotificationRepository(database.db),
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )

        test("får ikke opprette avtale dersom det oppstår valideringsfeil") {
            val request = AvtaleFixtures.avtaleRequest

            every { validator.validate(any(), any()) } returns listOf(
                ValidationError("navn", "Dårlig navn"),
            ).left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(ValidationError("navn", "Dårlig navn")),
            )
        }

        test("får ikke opprette avtale dersom virksomhet ikke finnes i Brreg") {
            val request = AvtaleFixtures.avtaleRequest.copy(
                arrangorOrganisasjonsnummer = Organisasjonsnummer("888777435"),
                arrangorUnderenheter = listOf(),
            )

            coEvery { brregClient.getBrregVirksomhet(Organisasjonsnummer("888777435")) } returns BrregError.NotFound.left()

            avtaleService.upsert(request, bertilNavIdent).shouldBeLeft(
                listOf(
                    ValidationError(
                        "arrangorOrganisasjonsnummer",
                        "Tiltaksarrangøren finnes ikke i Brønnøysundregistrene",
                    ),
                ),
            )
        }
    }

    context("Avbryte avtale") {
        val arrangorService = ArrangorService(mockk(), ArrangorRepository(database.db))
        val avtaleRepository = AvtaleRepository(database.db)
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaleService = AvtaleService(
            avtaleRepository,
            tiltaksgjennomforinger,
            arrangorService,
            NotificationRepository(database.db),
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )

        test("Man skal ikke få avbryte dersom avtalen ikke finnes") {
            val avtale = AvtaleFixtures.oppfolging.copy(navn = "Avtale som eksisterer")
            val avtaleIdSomIkkeFinnes = "3c9f3d26-50ec-45a7-a7b2-c2d8a3653945".toUUID()
            avtaleRepository.upsert(avtale)

            avtaleService.avbrytAvtale(avtaleIdSomIkkeFinnes, bertilNavIdent, AvbruttAarsak.Feilregistrering)
                .shouldBeLeft(NotFound("Avtalen finnes ikke"))
        }

        test("Man skal få avbryte når opphav for avtalen er Arena") {
            val avtaler = AvtaleRepository(database.db)
            val service = AvtaleService(
                avtaleRepository,
                tiltaksgjennomforinger,
                arrangorService,
                NotificationRepository(database.db),
                validator,
                EndringshistorikkService(database.db),
                database.db,
            )
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
            )
            avtaler.upsert(avtale)
            avtaler.setOpphav(avtale.id, ArenaMigrering.Opphav.ARENA)

            service.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeRight()
        }

        test("Man skal ikke få avbryte, men få en melding dersom avtalen allerede er avsluttet") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = LocalDate.of(2023, 6, 1),
            )
            avtaleRepository.upsert(avtale)
            avtaleRepository.setOpphav(avtale.id, ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                BadRequest(message = "Avtalen er allerede avsluttet og kan derfor ikke avbrytes."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.of(2024, 5, 17),
                sluttDato = LocalDate.of(2025, 7, 1),
            )
            avtaleRepository.upsert(avtale)
            val oppfolging1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            val oppfolging2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.of(2023, 5, 1),
                sluttDato = null,
            )
            tiltaksgjennomforinger.upsert(oppfolging1)
            tiltaksgjennomforinger.upsert(oppfolging2)

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                BadRequest("Avtalen har 2 aktive tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan avbryte avtalen."),
            )
        }

        test("Man skal ikke få avbryte, men få en melding dersom det finnes planlagte gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusMonths(1),
            )
            avtaleRepository.upsert(avtale)
            val oppfolging1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.now().plusDays(1),
                sluttDato = null,
            )
            val oppfolging2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.now().plusDays(1),
                sluttDato = null,
            )

            tiltaksgjennomforinger.upsert(oppfolging1)
            tiltaksgjennomforinger.upsert(oppfolging2)

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeLeft(
                BadRequest("Avtalen har 2 planlagte tiltaksgjennomføringer koblet til seg. Du må flytte eller avslutte gjennomføringene før du kan avbryte avtalen."),
            )
        }

        test("Man skal få avbryte dersom det ikke finnes aktive gjennomføringer koblet til avtalen") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                id = UUID.randomUUID(),
                navn = "Avtale som eksisterer",
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().plusMonths(1),
            )
            avtaleRepository.upsert(avtale)
            val oppfolging1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                avtaleId = avtale.id,
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().minusDays(1),
            )
            tiltaksgjennomforinger.upsert(oppfolging1)

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering).shouldBeRight()
        }

        test("Skal få avbryte avtale hvis alle sjekkene er ok") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                startDato = LocalDate.now().minusDays(1),
                sluttDato = LocalDate.now().plusMonths(1),
            )
            avtaleRepository.upsert(avtale).right()

            avtaleService.avbrytAvtale(avtale.id, bertilNavIdent, AvbruttAarsak.Feilregistrering)
        }
    }

    context("Administrator-notification") {
        val arrangorService = ArrangorService(mockk(), ArrangorRepository(database.db))
        val tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)
        val avtaler = AvtaleRepository(database.db)
        val avtaleService = AvtaleService(
            avtaler,
            tiltaksgjennomforinger,
            arrangorService,
            NotificationRepository(database.db),
            validator,
            EndringshistorikkService(database.db),
            database.db,
        )
        val navAnsattRepository = NavAnsattRepository(database.db)

        test("Ingen administrator-notification hvis administrator er samme som opprettet") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = bertilNavIdent,
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
            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(bertilNavIdent))
            avtaleService.upsert(avtale, bertilNavIdent)

            database.assertThat("user_notification").isEmpty
        }

        test("Bare nye administratorer får notification når man endrer gjennomføring") {
            navAnsattRepository.upsert(
                NavAnsattDbo(
                    navIdent = bertilNavIdent,
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
                    navIdent = NavIdent("Z654321"),
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
                    navIdent = NavIdent("T654321"),
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
            val avtale = AvtaleFixtures.avtaleRequest.copy(administratorer = listOf(NavIdent("Z654321")))
            avtaleService.upsert(avtale, bertilNavIdent)

            database.assertThat("user_notification")
                .hasNumberOfRows(1)
                .column("user_id")
                .containsValues("Z654321")

            avtaleService.upsert(
                avtale.copy(
                    navn = "nytt navn",
                    administratorer = listOf(NavIdent("Z654321"), NavIdent("T654321"), bertilNavIdent),
                ),
                bertilNavIdent,
            )

            database.assertThat("user_notification")
                .hasNumberOfRows(2)
                .column("user_id")
                .containsValues("Z654321", "T654321")
        }
    }
})
